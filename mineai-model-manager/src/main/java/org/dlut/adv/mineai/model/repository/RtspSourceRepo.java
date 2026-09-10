package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.core.entity.RtspSource;
import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RtspSourceRepo extends PagingAndSortingRepository<RtspSource, Long>, JpaSpecificationExecutor<RtspSource> {

    @Query("SELECT r FROM RtspSource r WHERE r.isDelete = 0 ORDER BY r.id DESC")
    List<RtspSource> findAllAvailable();

    @Query("SELECT r FROM RtspSource r WHERE " +
            "r.isDelete = 0 AND " +
            "(COALESCE(:name, '') = '' OR r.name LIKE %:name%) AND " +
            "(COALESCE(:rtspUrl, '') = '' OR r.rtspUrl LIKE %:rtspUrl%) AND " +
            "(COALESCE(:username, '') = '' OR r.username LIKE %:username%) AND " +
            "(COALESCE(:description, '') = '' OR r.description LIKE %:description%)")
    Page<RtspSource> dynamicFindRtspSourcePage(@Param("name") String name,
                                               @Param("rtspUrl") String rtspUrl,
                                               @Param("username") String username,
                                               @Param("description") String description,
                                               Pageable pageable);
}
