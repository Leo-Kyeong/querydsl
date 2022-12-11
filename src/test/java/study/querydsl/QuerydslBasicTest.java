package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
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
import javax.persistence.PersistenceContext;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @PersistenceContext
    EntityManager em;
    JPAQueryFactory jpaQueryFactory;

    @BeforeEach
    public void before() {
        // JPAQueryFactory init
        jpaQueryFactory = new JPAQueryFactory(em);

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
        //given
        QMember member = QMember.member;

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
        //given
        QMember member = QMember.member;

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
        //given
    	QMember member = QMember.member;

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
        QMember member = QMember.member;

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
        //given
        QMember member = QMember.member;

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
        //given
    	QMember member = QMember.member;

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
        //given
    	QMember member = QMember.member;
        QTeam team = QTeam.team;

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
}
