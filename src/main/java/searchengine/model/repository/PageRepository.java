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

    @Transactional
    @Query("SELECT p FROM PageEntity p WHERE p.path = :path")
    PageEntity findByPath(@Param("path") String path);

    @Transactional
    @Query(value = "SELECT p.content FROM search_engine.page p " +
            "WHERE p.path = :path", nativeQuery = true)
    String getContent(@Param("path") String path);

    @Transactional
    @Query(value = "SELECT p.path FROM search_engine.page p " +
            "WHERE p.content = :content", nativeQuery = true)
    String getPath(@Param("content") String content);
}
