package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import searchengine.model.entity.PageEntity;
import searchengine.model.entity.SiteEntity;

import javax.transaction.Transactional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    @Async
    @Transactional
    @Query(value = "SELECT p.path FROM search_engine.page p " +
            "WHERE p.site_id = :site_id ORDER BY id DESC LIMIT 1", nativeQuery = true)
    String getLastSitePage(@Param("site_id") SiteEntity site);

    @Async
    @Transactional
    boolean existsByPath(String path);
}
