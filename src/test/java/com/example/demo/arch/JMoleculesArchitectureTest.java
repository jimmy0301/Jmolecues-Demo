package com.example.demo.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Set;
import org.jmolecules.architecture.cqrs.Command;
import org.jmolecules.architecture.cqrs.CommandHandler;
import org.jmolecules.architecture.cqrs.QueryModel;
import org.jmolecules.architecture.onion.classical.ApplicationServiceRing;
import org.jmolecules.archunit.JMoleculesArchitectureRules;
import org.jmolecules.archunit.JMoleculesDddRules;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Repository;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.event.annotation.DomainEvent;
import org.junit.jupiter.api.Test;

class JMoleculesArchitectureTest {

    private static final String[] API_DTO_PACKAGE_PATTERNS = {"api.model", "generated"};

    private static final Set<Class<? extends Annotation>> DDD_BUILDING_BLOCK_ANNOTATIONS =
            Set.of(AggregateRoot.class, Entity.class, ValueObject.class, DomainEvent.class);

    private final com.tngtech.archunit.core.domain.JavaClasses classes =
            new ClassFileImporter().importPackages("com.example.demo");

    /**
     * 驗證 DDD building block 的使用規則： - @AggregateRoot 的 ID 必須實作 Identifier - @Repository
     * 只能管理 @AggregateRoot - @ValueObject 不能有可變狀態 - Bounded Context 之間只能透過 ID 參照
     */
    @Test
    void shouldFollowDddRules() {
        JMoleculesDddRules.all().check(classes);
    }

    /** 驗證 Onion Architecture（Classical）環狀依賴方向：內圈不可依賴外圈 */
    @Test
    void shouldFollowOnionArchitecture() {
        JMoleculesArchitectureRules.ensureOnionClassical().check(classes);
    }

    /** 規則 #2：@Repository 只能管理 @AggregateRoot，不可管理普通 @Entity */
    @Test
    void repositoriesShouldOnlyManageAggregateRoots() {
        ArchCondition<JavaClass> onlyManageAggregateRoots =
                new ArchCondition<>("only manage types annotated with @AggregateRoot") {
                    @Override
                    public void check(JavaClass repositoryClass, ConditionEvents events) {
                        repositoryClass.getInterfaces().stream()
                                .filter(type -> type instanceof JavaParameterizedType)
                                .map(type -> (JavaParameterizedType) type)
                                .filter(
                                        type ->
                                                type.toErasure()
                                                        .isAssignableTo(
                                                                "org.springframework.data.repository.Repository"))
                                .flatMap(type -> type.getActualTypeArguments().stream())
                                .findFirst()
                                .ifPresent(
                                        managedType -> {
                                            JavaClass managed = managedType.toErasure();
                                            if (!managed.isAnnotatedWith(AggregateRoot.class)) {
                                                events.add(
                                                        SimpleConditionEvent.violated(
                                                                repositoryClass,
                                                                String.format(
                                                                        "@Repository %s manages %s"
                                                                                + " which is not annotated with @AggregateRoot",
                                                                        repositoryClass
                                                                                .getSimpleName(),
                                                                        managed.getSimpleName())));
                                            }
                                        });
                    }
                };

        classes()
                .that()
                .areAnnotatedWith(Repository.class)
                .should(onlyManageAggregateRoots)
                .check(classes);
    }

    /**
     * 規則 #4：@Command 必須不可變 — 所有欄位需為 final，且不可有 setter
     *
     * <p>Command 代表某一時刻的操作意圖，可變 Command 可能被修改後重送，造成狀態不一致。
     *
     * <p>預期失敗（violation demo）：BadCommand — 違規 #4（可變 Command）
     */
    @Test
    void commandsShouldBeImmutable() {
        ArchCondition<JavaClass> beImmutable =
                new ArchCondition<>("be immutable — no non-final fields and no setters") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        if (javaClass.isRecord()) return;

                        javaClass.getFields().stream()
                                .filter(f -> !f.getModifiers().contains(JavaModifier.FINAL))
                                .filter(f -> !f.getModifiers().contains(JavaModifier.STATIC))
                                .forEach(
                                        f ->
                                                events.add(
                                                        SimpleConditionEvent.violated(
                                                                javaClass,
                                                                String.format(
                                                                        "@Command %s has non-final field '%s'",
                                                                        javaClass.getSimpleName(),
                                                                        f.getName()))));

                        javaClass.getMethods().stream()
                                .filter(m -> m.getName().startsWith("set"))
                                .forEach(
                                        m ->
                                                events.add(
                                                        SimpleConditionEvent.violated(
                                                                javaClass,
                                                                String.format(
                                                                        "@Command %s has setter '%s'",
                                                                        javaClass.getSimpleName(),
                                                                        m.getName()))));
                    }
                };

        classes().that().areAnnotatedWith(Command.class).should(beImmutable).check(classes);
    }

    /**
     * 規則 #3：@ValueObject 必須不可變 — 所有欄位需為 final，且不可有 setter
     *
     * <p>兩層檢查：
     *
     * <ol>
     *   <li>non-final instance field：涵蓋所有可變情境，包含 Lombok @Setter / @Data（因為 Lombok setter 必須搭配非
     *       final 欄位，這層已足夠）
     *   <li>bytecode 中有 set* 方法：補捉明確手寫的 setter
     * </ol>
     *
     * <p>注意：Lombok @Setter 的 RetentionPolicy 是 SOURCE，annotation 不進 bytecode， 無法直接透過 ArchUnit 偵測；但
     * Lombok setter 必須搭配非 final 欄位，第一層已涵蓋。
     */
    @Test
    void valueObjectsShouldBeImmutable() {
        ArchCondition<JavaClass> beImmutable =
                new ArchCondition<>("be immutable — no non-final fields and no setters") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        if (javaClass.isRecord()) return;

                        // 1. Non-final instance field — 涵蓋手寫可變欄位與 Lombok @Setter / @Data
                        javaClass.getFields().stream()
                                .filter(f -> !f.getModifiers().contains(JavaModifier.FINAL))
                                .filter(f -> !f.getModifiers().contains(JavaModifier.STATIC))
                                .forEach(
                                        f ->
                                                events.add(
                                                        SimpleConditionEvent.violated(
                                                                javaClass,
                                                                String.format(
                                                                        "@ValueObject %s has non-final field '%s'",
                                                                        javaClass.getSimpleName(),
                                                                        f.getName()))));

                        // 2. bytecode 中明確的 set* 方法
                        javaClass.getMethods().stream()
                                .filter(m -> m.getName().startsWith("set"))
                                .forEach(
                                        m ->
                                                events.add(
                                                        SimpleConditionEvent.violated(
                                                                javaClass,
                                                                String.format(
                                                                        "@ValueObject %s has setter '%s'",
                                                                        javaClass.getSimpleName(),
                                                                        m.getName()))));
                    }
                };

        classes().that().areAnnotatedWith(ValueObject.class).should(beImmutable).check(classes);
    }

    /**
     * 規則 #5：@QueryModel 不可呼叫 @CommandHandler 方法
     *
     * <p>CQRS 核心規則：read side 只能讀取狀態，不可觸發 state change。
     *
     * <p>預期失敗（violation demo）：BadQueryModel — 違規 #5（query 呼叫 command handler）
     */
    @Test
    void queryModelsShouldNotTriggerCommands() {
        ArchCondition<JavaClass> notCallCommandHandlers =
                new ArchCondition<>("not call methods annotated with @CommandHandler") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        javaClass.getMethodCallsFromSelf().stream()
                                .filter(
                                        call ->
                                                call.getTarget()
                                                        .resolveMember()
                                                        .filter(
                                                                m ->
                                                                        m.isAnnotatedWith(
                                                                                CommandHandler
                                                                                        .class))
                                                        .isPresent())
                                .forEach(
                                        call ->
                                                events.add(
                                                        SimpleConditionEvent.violated(
                                                                javaClass,
                                                                String.format(
                                                                        "@QueryModel %s calls"
                                                                                + " @CommandHandler"
                                                                                + " '%s.%s'",
                                                                        javaClass.getSimpleName(),
                                                                        call.getTarget()
                                                                                .getOwner()
                                                                                .getSimpleName(),
                                                                        call.getTarget()
                                                                                .getName()))));
                    }
                };

        classes()
                .that()
                .areAnnotatedWith(QueryModel.class)
                .should(notCallCommandHandlers)
                .check(classes);
    }

    /**
     * 規則 #6：@Command 必須位於 @ApplicationServiceRing 標記的 package
     *
     * <p>Command 是應用層的輸入物件。規則以 package-info 的 @ApplicationServiceRing 為準，不綁定固定資料夾名稱。
     *
     * <p>預期失敗（violation demo）：BadCommand — 違規 #6（Command 放在未標記 @ApplicationServiceRing 的 context
     * root）
     */
    @Test
    void commandsShouldResideInApplicationServiceRing() {
        classes()
                .that()
                .areAnnotatedWith(Command.class)
                .should(resideInPackageAnnotatedWith(ApplicationServiceRing.class))
                .check(classes);
    }

    /**
     * 規則 #7：@CommandHandler 方法只能在 @ApplicationServiceRing 標記的 package
     *
     * <p>CommandHandler 協調 repository 與 domain event，屬應用層職責；出現在未標記 @ApplicationServiceRing 的
     * package 代表層級邊界被打破。
     *
     * <p>預期失敗（violation demo）：BadDomainHandler — 違規 #7（CommandHandler 在未標記 @ApplicationServiceRing
     * 的 ordering root）
     */
    @Test
    void commandHandlersShouldBeInApplicationLayer() {
        ArchCondition<JavaMethod> beDeclaredInApplicationServiceRing =
                new ArchCondition<>(
                        "be declared in a package annotated with @ApplicationServiceRing") {
                    @Override
                    public void check(JavaMethod method, ConditionEvents events) {
                        JavaClass owner = method.getOwner();
                        if (!owner.getPackage().isAnnotatedWith(ApplicationServiceRing.class)) {
                            events.add(
                                    SimpleConditionEvent.violated(
                                            method,
                                            String.format(
                                                    "@CommandHandler %s.%s is declared in package %s"
                                                            + " which is not annotated with @ApplicationServiceRing",
                                                    owner.getSimpleName(),
                                                    method.getName(),
                                                    owner.getPackageName())));
                        }
                    }
                };

        methods()
                .that()
                .areAnnotatedWith(CommandHandler.class)
                .should(beDeclaredInApplicationServiceRing)
                .check(classes);
    }

    /**
     * 規則 #8：DDD building block 不依賴 API DTO / generated model
     *
     * <p>Domain model 應表達領域語言，不依賴 transport contract。API DTO package 名稱集中在
     * API_DTO_PACKAGE_PATTERNS，未來若 generated package 改名，只需調整該設定。
     */
    @Test
    void dddBuildingBlocksShouldNotDependOnApiDtos() {
        ArchCondition<JavaClass> notDependOnApiDtos =
                new ArchCondition<>("not depend on API DTO or generated model packages") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        if (!isDddBuildingBlock(javaClass)) {
                            return;
                        }

                        javaClass.getDirectDependenciesFromSelf().stream()
                                .filter(
                                        dependency ->
                                                isApiDtoPackage(
                                                        dependency
                                                                .getTargetClass()
                                                                .getPackageName()))
                                .forEach(
                                        dependency ->
                                                events.add(
                                                        SimpleConditionEvent.violated(
                                                                javaClass,
                                                                String.format(
                                                                        "DDD building block %s depends on API DTO/generated type %s",
                                                                        javaClass.getSimpleName(),
                                                                        dependency
                                                                                .getTargetClass()
                                                                                .getName()))));
                    }
                };

        classes().should(notDependOnApiDtos).check(classes);
    }

    private static ArchCondition<JavaClass> resideInPackageAnnotatedWith(
            Class<? extends Annotation> annotation) {
        return new ArchCondition<>(
                "reside in a package annotated with @" + annotation.getSimpleName()) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                if (!javaClass.getPackage().isAnnotatedWith(annotation)) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    javaClass,
                                    String.format(
                                            "%s resides in package %s which is not annotated with @%s",
                                            javaClass.getSimpleName(),
                                            javaClass.getPackageName(),
                                            annotation.getSimpleName())));
                }
            }
        };
    }

    private static boolean isDddBuildingBlock(JavaClass javaClass) {
        return DDD_BUILDING_BLOCK_ANNOTATIONS.stream().anyMatch(javaClass::isAnnotatedWith);
    }

    private static boolean isApiDtoPackage(String packageName) {
        return Arrays.stream(API_DTO_PACKAGE_PATTERNS).anyMatch(packageName::contains);
    }
}
