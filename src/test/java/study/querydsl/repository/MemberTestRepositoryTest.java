package study.querydsl.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

@Transactional
@SpringBootTest
class MemberTestRepositoryTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    MemberTestRepository repository;

    @Test
    @DisplayName("Page 테스트")
    void searchPageSimple() {
        //given
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        entityManager.persist(teamA);
        entityManager.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        entityManager.persist(member1);
        entityManager.persist(member2);
        entityManager.persist(member3);
        entityManager.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setTeamName("teamA");
        PageRequest pageRequest = PageRequest.of(0, 3);

        //when
        Page<Member> result = repository.applyPagination2(condition, pageRequest);

        //then
        for (Member member : result) {
            System.out.println("member = " + member);
        }
    }
}