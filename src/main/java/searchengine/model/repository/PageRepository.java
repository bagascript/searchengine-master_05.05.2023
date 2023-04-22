package searchengine.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.entity.PageEntity;
import searchengine.model.entity.SiteEntity;

import javax.transaction.Transactional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer>
{
    @Transactional
    @Query(value = "SELECT case WHEN search_engine.page.code = 200 " +
            "THEN true else false end FROM search_engine.page " +
            "WHERE search_engine.page.site_id = :site_id",  nativeQuery = true)
    int checkStatusCodePage(@Param("site_id") SiteEntity site);
}
