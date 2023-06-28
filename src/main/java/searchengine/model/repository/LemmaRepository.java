package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.entity.LemmaEntity;

import javax.transaction.Transactional;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer>
{

    @Transactional
    boolean existsByLemma(String lemma);

    @Transactional
    @Query(value = "SELECT l FROM LemmaEntity l WHERE l.lemma = :lemma")
    LemmaEntity getLemma(@Param("lemma") String lemma);

    @Modifying
    @Transactional
    @Query("UPDATE LemmaEntity l SET l.frequency = :frequency WHERE l.id = :id")
    void updateFrequency(@Param("id") int id, int frequency);


}
