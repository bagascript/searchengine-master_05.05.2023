package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.entity.SiteEntity;
import searchengine.model.entity.StatusType;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer>
{
    @Modifying
    @Transactional
    @Query("UPDATE SiteEntity s SET s.statusTime = now() WHERE s.id = :id")
    void updateStatusTime(@Param("id") int id);

    @Modifying
    @Transactional
    @Query("UPDATE SiteEntity s SET s.status = :status WHERE s.id = :id")
    void updateOnIndexed(@Param("id") int id, StatusType status);

    @Modifying
    @Transactional
    @Query("UPDATE SiteEntity s SET s.status = :status, s.lastError = :lastError WHERE s.id = :id")
    void updateOnFailed(@Param("id") int id, StatusType status, String lastError);

    @Transactional
    @Query("SELECT s FROM SiteEntity s WHERE s.name = :name AND s.status = :status")
    Optional<SiteEntity> findByNameAndStatus(@Param("name") String name, @Param("status") StatusType indexed);

    @Modifying
    @Transactional
    List<SiteEntity> findAllByStatus(StatusType statusType);

    @Transactional
    @Query("SELECT s FROM SiteEntity s WHERE s.url = :url")
    SiteEntity getSiteId(@Param("url") String url);
}
