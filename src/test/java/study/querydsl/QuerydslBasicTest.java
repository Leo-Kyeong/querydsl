package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @PersistenceContext
    EntityManager em;

    JPAQueryFactory jpaQueryFactory;

    QMember member;

    QTeam team;

    @BeforeEach
    public void before() {
        // JPAQueryFactory init
        jpaQueryFactory = new JPAQueryFactory(em);

        member = QMember.member;
        team = QTeam.team;

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    @DisplayName("Member1 을 JPQL 을 통해 찾는 테스트")
    void startJPQL() {
        String query = "select m from Member m where m.userName = :userName";
        Member findMember = em.createQuery(query, Member.class)
                .setParameter("userName", "member1")
                .getSingleResult();

        assertThat(findMember.getUserName()).isEqualTo("member1");
    }

    @Test
    @DisplayName("Member1 을 Query DSL 을 통해 찾는 테스트")
    void startQuerydsl() {
        //given
        QMember m = new QMember("m");

        //when
        Member findMember = jpaQueryFactory
                .select(m)
                .from(m)
                .where(m.userName.eq("member1")) // 파라미터 바인딩 처리
                .fetchOne();

        //then
        assertThat(findMember.getUserName()).isEqualTo("member1");
    }

    @Test
    void search() {
        //when
        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.userName.eq("member1")
                        .and(member.age.between(10, 30)))
                .fetchOne();

        //then
        assertThat(findMember.getUserName()).isEqualTo("member1");
    }

    @Test
    void searchAndParam() {
        //when
        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .where(
                        member.userName.eq("member1"),
                        member.age.between(10, 30)
                )
                .fetchOne();

        //then
        assertThat(findMember.getUserName()).isEqualTo("member1");
    }

    @Test
    void resultFetch() {
        //when
        List<Member> fetch = jpaQueryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = jpaQueryFactory
                .selectFrom(member)
                .fetchOne();

        Member fetchFirst = jpaQueryFactory
                .selectFrom(member)
//                .limit(1).fetchOne();
                .fetchFirst();

        QueryResults<Member> results = jpaQueryFactory
                .selectFrom(member)
                .fetchResults();

        long total = results.getTotal();
        List<Member> content = results.getResults();

        long fetchCount = jpaQueryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 3. 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    void sort() {
        //given
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        //when
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.userName.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        //then
        assertThat(member5.getUserName()).isEqualTo("member5");
        assertThat(member6.getUserName()).isEqualTo("member6");
        assertThat(memberNull.getUserName()).isNull();
    }

    @Test
    void paging() {
        //when
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .orderBy(member.userName.desc())
                .offset(1)
                .limit(2)
                .fetch();

        //then
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    void aggregation() {
        //when
        List<Tuple> result = jpaQueryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);

        //then
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    void group() {
        //when
        List<Tuple> result = jpaQueryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        //then
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); // (10 + 20) / 2

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); // (30 + 40) / 2
    }

    /**
     * 팀 A 에 소속된 모든 회원
     */
    @Test
    void join() {
        //when
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        //then
        assertThat(result)
                .extracting("userName")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인(연관관계가 없는 필드로 조인)
     * 회원의 이름이 팀 이름과 같은 회원 조회
     * -> 모든 멤버과 모든 팀을 가져와 조인하고 조건(where)에 맞게 조회한다.
     * -> DB 가 성능 최적화를 해준다.
     */
    @Test
    void theta_join() {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        //when
        List<Member> result = jpaQueryFactory
                .select(member)
                .from(member, team)
                .where(member.userName.eq(team.name))
                .fetch();

        //then
        assertThat(result)
                .extracting("userName")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA 인 팀만 조인, 회원은 모두 조회
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL : SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID = t.id and t.name = 'teamA'
     */
    @Test
    void join_on_filtering() {
        //given

        //when
        List<Tuple> result = jpaQueryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                // .where(team.name.eq("teamA"))
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        // Console Log
        // tuple = [Member(id=3, userName=member1, age=10), Team(id=1, name=teamA)]
        // tuple = [Member(id=4, userName=member2, age=20), Team(id=1, name=teamA)]
        // tuple = [Member(id=5, userName=member3, age=30), null]
        // tuple = [Member(id=6, userName=member4, age=40), null]
    }

    /**
     * 연관관계 없는 엔티티 외부 조인
     * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     */
    @Test
    void join_on_no_relation() {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        //when
        List<Tuple> result = jpaQueryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.userName.eq(team.name))
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        // Console Log
        // tuple = [Member(id=3, userName=member1, age=10), null]
        // tuple = [Member(id=4, userName=member2, age=20), null]
        // tuple = [Member(id=5, userName=member3, age=30), null]
        // tuple = [Member(id=6, userName=member4, age=40), null]
        // tuple = [Member(id=7, userName=teamA, age=0), Team(id=1, name=teamA)]
        // tuple = [Member(id=8, userName=teamB, age=0), Team(id=2, name=teamB)]
        // tuple = [Member(id=9, userName=teamC, age=0), null]
    }

    @PersistenceUnit
    EntityManagerFactory entityManagerFactory;

    @Test
    void fetchJoinNo() {
        //given
        em.flush();
        em.clear();

        //when
        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.userName.eq("member1"))
                .fetchOne();

        //then
        boolean loaded = entityManagerFactory.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    void fetchJoinUse() {
        //given
        em.flush();
        em.clear();

        //when
        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.userName.eq("member1"))
                .fetchOne();

        //then
        boolean loaded = entityManagerFactory.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /**
     * 예) 나이가 가장 많은 회원 조회
     */
    @Test
    void subQuery() {
        //given
        QMember memberSub = new QMember("memberSub");

        //when
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions.select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        //then
        assertThat(result)
                .extracting("age")
                .containsExactly(40);
    }

    /**
     * 예) 나이가 평균 이상인 회원
     */
    @Test
    void subQueryGoe() {
        //given
        QMember memberSub = new QMember("memberSub");

        //when
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions.select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        //then
        assertThat(result)
                .extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * 서브쿼리 여러 건 처리, in 사용
     */
    @Test
    void subQueryIn() {
        //given
        QMember memberSub = new QMember("memberSub");

        //when
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions.select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        //then
        assertThat(result)
                .extracting("age")
                .containsExactly(20, 30, 40);
    }

    /**
     * select 절에 사용하는 서브쿼리 (스칼라 서브쿼리)
     */
    @Test
    void selectSubQuery() {
        //given
        QMember memberSub = new QMember("memberSub");

        //when
        List<Tuple> result = jpaQueryFactory
                .select(member.userName,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        // Console Log
        // tuple = [member1, 25.0]
        // tuple = [member2, 25.0]
        // tuple = [member3, 25.0]
        // tuple = [member4, 25.0]
    }

    @Test
    void basicCase() {
        //given

        //when
        List<String> result = jpaQueryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        //then
        for (String age : result) {
            System.out.println("age = " + age);
        }

        // Console Log
        // age = 열살
        // age = 스무살
        // age = 기타
        // age = 기타
    }

    /**
     * 복잡한 조건
     * CaseBuilder 사용
     */
    @Test
    void complexCase() {
        //given

        //when
        List<String> result = jpaQueryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        //then
        for (String age : result) {
            System.out.println("age = " + age);
        }
    }

    /**
     * orderBy 에서 Case 문 함께 사용
     * 다음과 같은 임의의 순서로 회원을 출력하고 싶다면?
     * 0 ~ 30살이 아닌 회원을 가장 먼저 출력
     * 0 ~ 20살 회원 출력
     * 21 ~ 30살 회원 출력
     */
    @Test
    void orderByCase() {
        //given

        //when
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);

        List<Tuple> result = jpaQueryFactory
                .select(member.userName, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();

        //then
        for (Tuple tuple : result) {
            String userName = tuple.get(member.userName);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);
            System.out.println("userName = " + userName + " age = " + age + " rank = " + rank);
        }
    }

    @Test
    void constant() {
        //given

        //when
        List<Tuple> result = jpaQueryFactory
                .select(member.userName, Expressions.constant("A"))
                .from(member)
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void concat() {
        //given

        //when
        List<String> result = jpaQueryFactory
                .select(member.userName.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.userName.eq("member1"))
                .fetch();

        //then
        for (String concat : result) {
            System.out.println("concat = " + concat); // concat = member1_10
        }
    }
}
