package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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
}
