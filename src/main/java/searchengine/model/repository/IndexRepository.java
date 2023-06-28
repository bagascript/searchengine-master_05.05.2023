package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.entity.IndexEntity;



import javax.transaction.Transactional;


public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    @Modifying
    @Transactional
    @Query("delete from IndexEntity i where i.id = :id")
    void deleteId(@Param("id") int id);
}
