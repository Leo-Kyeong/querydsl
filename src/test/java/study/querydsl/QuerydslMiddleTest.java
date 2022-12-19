package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
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
public class QuerydslMiddleTest {

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
    void simpleProjection() {
        //given

        //when
        List<String> result = jpaQueryFactory
                .select(member.userName)
                .from(member)
                .fetch();

        //then
        for (String userName : result) {
            System.out.println("userName = " + userName);
        }
    }

    @Test
    void tupleProjection() {
        //given

        //when
        List<Tuple> result = jpaQueryFactory
                .select(member.userName, member.age)
                .from(member)
                .fetch();

        //then
        for (Tuple tuple : result) {
            String userName = tuple.get(member.userName);
            Integer age = tuple.get(member.age);
            System.out.println("userName = " + userName);
            System.out.println("age = " + age);
        }
    }

    @Test
    void findDtoByJPQL() {
        //given

        //when
        String qlString = "select new study.querydsl.dto.MemberDto(m.userName, m.age) from Member m";

        List<MemberDto> result = em.createQuery(qlString, MemberDto.class)
                .getResultList();

        //then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findDtoBySetter() {
        //given

        //when
        List<MemberDto> result = jpaQueryFactory
                .select(Projections.bean(MemberDto.class,
                        member.userName,
                        member.age))
                .from(member)
                .fetch();

        //then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findDtoByField() {
        //given

        //when
        List<MemberDto> result = jpaQueryFactory
                .select(Projections.fields(MemberDto.class,
                        member.userName,
                        member.age))
                .from(member)
                .fetch();

        //then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findDtoByConstructor() {
        //given

        //when
        List<MemberDto> result = jpaQueryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.userName,
                        member.age))
                .from(member)
                .fetch();

        //then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findUserDtoByField() {
        //given
        QMember memberSub = new QMember("memberSub");

        //when
        List<UserDto> result = jpaQueryFactory
                .select(Projections.fields(UserDto.class,
                        member.userName.as("name"),
                        ExpressionUtils.as(
                                JPAExpressions
                                    .select(memberSub.age.max())
                                    .from(memberSub), "age")
                        )
                ).from(member)
                .fetch();

        //then
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    void findDtoByQueryProjection() {
        //given

        //when
        List<MemberDto> result = jpaQueryFactory
                .select(new QMemberDto(member.userName, member.age))
                .from(member)
                .fetch();

        //then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void dynamicQuery_BooleanBuilder() {
        //given
        String userNameParam = "member1";
        Integer ageParam = 10;

        //when
        List<Member> result = searchMember(userNameParam, ageParam);

        //then
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember(String userNameParam, Integer ageParam) {

        // 생성자를 통해 초기 값을 넣을 수도 있다.
        BooleanBuilder builder = new BooleanBuilder();
        if (userNameParam != null) {
            builder.and(member.userName.eq(userNameParam));
        }

        if (ageParam != null) {
            builder.and(member.age.eq(ageParam));
        }

        return jpaQueryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    void dynamicQuery_WhereParam() {
        //given
        String userNameParam = "member1";
        Integer ageParam = 10;

        //when
        List<Member> result = searchMember2(userNameParam, ageParam);

        //then
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String userNameParam, Integer ageParam) {
        return jpaQueryFactory
                .selectFrom(member)
                .where(userNameEq(userNameParam), ageEq(ageParam)) // where 조건에 null 은 무시됨
                .fetch();
    }

    private BooleanExpression userNameEq(String userNameParam) {
        return userNameParam != null ? member.userName.eq(userNameParam) : null;
    }

    private BooleanExpression ageEq(Integer ageParam) {
        return ageParam != null ? member.age.eq(ageParam) : null;
    }

    private BooleanExpression allEq(String userNameParam, Integer ageParam) {
        return userNameEq(userNameParam).and(ageEq(ageParam));
    }

    /**
     * 멤버 나이가 28 보다 작다면 멤버 이름을 "비회원" 으로 변경
     */
    @Test
    void bulkUpdate() {
        //given

        //when
        long count = jpaQueryFactory
                .update(member)
                .set(member.userName, "비회원")
                .where(member.age.lt(28))
                .execute();


        em.clear();

        //then
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .fetch();

        for (Member findMember : result) {
            System.out.println("findMember = " + findMember);
        }
    }

    @Test
    void bulkAdd() {
        //given

        //when
        long count = jpaQueryFactory
                .update(member)
                .set(member.age, member.age.add(2)) // 빼고 싶다면 -2
                .execute();

        //then
    }

    @Test
    void bulkDelete() {
        //given

        //when
        long count = jpaQueryFactory
                .delete(member)
                .where(member.age.gt(21))
                .execute();

        //then
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .fetch();

        for (Member findMember : result) {
            System.out.println("findMember = " + findMember);
        }
    }
}
