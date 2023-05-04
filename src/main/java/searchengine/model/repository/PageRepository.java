package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.entity.PageEntity;
import searchengine.model.entity.SiteEntity;

import javax.transaction.Transactional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    @Transactional
    @Query(value = "SELECT p.path FROM search_engine.page p " +
            "WHERE p.site_id = :site_id ORDER BY id DESC LIMIT 1", nativeQuery = true)
    String getLastSitePage(@Param("site_id") SiteEntity site);

    @Transactional
    @Query(value = "SELECT case WHEN count(search_engine.page.path) > 0 " +
            "THEN true else false end FROM search_engine.page " +
            "WHERE search_engine.page.path = :path AND search_engine.page.site_id = :site_id", nativeQuery = true)
    long checkOnDuplicatePage(@Param("path") String path, @Param("site_id") SiteEntity site);
}
