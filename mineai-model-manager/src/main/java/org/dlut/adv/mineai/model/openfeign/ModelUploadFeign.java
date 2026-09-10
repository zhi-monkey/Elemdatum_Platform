package org.dlut.adv.mineai.model.openfeign;

import com.alibaba.fastjson.JSONObject;
import io.kubernetes.client.openapi.models.V1Job;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Component
@FeignClient(value = "mineai-container-manager")
public interface ModelUploadFeign {

    /*
    *
    *feign调用 根据monitorName返回monitor
    */
    @RequestMapping(value = "/harbor/imageUpload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    boolean uploadImage(@RequestPart(value = "file") MultipartFile file, @RequestParam String imageName, @RequestParam String tag);

    @RequestMapping("/job/createJob")
    V1Job createJob(@RequestParam String namespace, @RequestParam String jobName,@RequestParam String weightJob, @RequestParam String image, @RequestParam String datasetPath, @RequestParam String weightPath, @RequestParam int gpuNum, @RequestBody Map<String, String> params);

    @RequestMapping("/job/getJob")
    JSONObject getJob(@RequestParam String namespace, @RequestParam String jobName);
}
