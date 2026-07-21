package com.aivle13.fin_audit_ai.global.exception.file;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;

public class FileUploadFailedException extends BusinessException {
    public FileUploadFailedException() {
        super(ErrorCode.FILE_UPLOAD_FAILED);
    }

    public FileUploadFailedException(Throwable cause) {
        super(ErrorCode.FILE_UPLOAD_FAILED, cause);
    }

    public FileUploadFailedException(String filename, Throwable cause) {
        super(ErrorCode.FILE_UPLOAD_FAILED, "파일 업로드 실패: " + filename, cause);
    }
}
