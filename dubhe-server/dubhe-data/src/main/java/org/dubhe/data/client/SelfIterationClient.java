package org.dubhe.data.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "mineai-model-manager", contextId = "selfIterationClient")
public interface SelfIterationClient {

    @PostMapping("/selfIteration/callback/autoLabelComplete")
    String onAutoLabelComplete(@RequestHeader("Authorization") String authorization, @RequestParam("jobId") Long jobId, @RequestParam("datasetId") Long datasetId);

    @PostMapping("/selfIteration/callback/autoLabelFailed")
    String onAutoLabelFailed(@RequestHeader("Authorization") String authorization, @RequestParam("jobId") Long jobId, @RequestParam("datasetId") Long datasetId);
}
