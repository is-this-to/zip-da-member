package com.zipdamember.domain.file.response;

import com.zipdamember.domain.file.entity.FileObject;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파일 업로드 응답")
public record FileUploadResponse(
        @Schema(description = "회원가입 시 전달할 파일 식별자", type = "string")
        String fileId,

        @Schema(description = "업로드한 파일 URI")
        String fileUri
) {
    public static FileUploadResponse from(FileObject fileObject) {
        return new FileUploadResponse(
                fileObject.getFileId().toString(),
                fileObject.getFileUri()
        );
    }
}
