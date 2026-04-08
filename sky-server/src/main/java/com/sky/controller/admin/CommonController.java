package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 通用的控制器
 */
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {
    @Autowired
    private AliOssUtil aliOssUtil;
    /**
     * 圖片上传後,因為是在服務器當中的一個路徑,所以設計成String類型,返回圖片的路徑
     * @param file 圖片文件,用MultipartFile(由Springboot封裝)接收,形參名必須和前端接收的參數名保持一致
     * @return Result對象
     */
    @PostMapping("/upload")
    @ApiOperation("圖片上傳")
    public Result<String> uplod(MultipartFile file)  {
        log.info("文件:{}", file);
        try{
            //原始文件名
            String originalFilename = file.getOriginalFilename();
            //擷取原始文件名後綴(獲取文件類型)
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            //構造新的文件名
            String objectName = UUID.randomUUID().toString()+extension;
            //文件請求路徑
            String filePath = aliOssUtil.upload(file.getBytes(), objectName);
            return Result.success(filePath);
        }catch (IOException e){
            log.error("文件上傳失敗",e);
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}