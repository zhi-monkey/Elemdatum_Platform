package org.dlut.adv.mineai.model.client;

import org.dlut.adv.mineai.core.vo.DataResponseBody;
import org.dlut.adv.mineai.model.dto.UserResourceDTO;
import org.dlut.adv.mineai.model.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Component
@FeignClient(value = "dubhe-admin")
public interface DubheUserFeign {

    @GetMapping("/users/findByIds")
    DataResponseBody<List<UserDTO>> findUsersByIds(@RequestHeader("Authorization")String authorization, @RequestParam(value = "ids") List<Long> ids);

    @GetMapping("/users/findById")
    DataResponseBody<UserDTO> findUserById(@RequestHeader("Authorization")String authorization, @RequestParam(value = "userId") Long userId);

    @PostMapping("/users/addUserResource")
    String addUserResource(@RequestHeader("Authorization")String authorization, @RequestBody UserResourceDTO userResourceDTO);

    @PostMapping("/users/releaseUserResource")
    String releaseUserResource(@RequestHeader("Authorization")String authorization, @RequestBody UserResourceDTO userResourceDTO);

    @GetMapping(value = "/users/findIdsByUsernameLike")
    DataResponseBody<List<Long>> findIdsByUsernameLike(@RequestHeader("Authorization")String authorization, @RequestParam(value = "userName") String userName);
}
