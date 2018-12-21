package com.dockop.s3.sftp.common;

import com.amazonaws.Protocol;

public class S3Config{

        private String endPoint;

        private Protocol protocol;

        public String getEndPoint() {
            return endPoint;
        }

        public void setEndPoint(String endPoint) {
            this.endPoint = endPoint;
        }

        public Protocol getProtocol() {
            return protocol;
        }

        public void setProtocol(Protocol protocol) {
            this.protocol = protocol;
        }
    }
