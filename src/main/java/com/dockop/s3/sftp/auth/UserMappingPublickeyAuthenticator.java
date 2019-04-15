package com.dockop.s3.sftp.auth;

import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.logging.AbstractLoggingBean;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import java.security.PublicKey;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * 类名称: UserMappingPublickeyAuthenticator <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 2019/3/22 下午10:38
 */
public class UserMappingPublickeyAuthenticator extends AbstractLoggingBean implements PublickeyAuthenticator {

    private final Map<String, Collection<? extends PublicKey>> keyMap;

    public UserMappingPublickeyAuthenticator(Map<String, Collection<? extends PublicKey>> keyMap) {
        this.keyMap = (keyMap == null) ? Collections.emptyMap() : keyMap;
    }

    public final Map<String, Collection<? extends PublicKey>> getKeyMap() {
        return keyMap;
    }

    @Override
    public boolean authenticate(String username, PublicKey key, ServerSession session) {
        Map<String, Collection<? extends PublicKey>> matchKeys= getKeyMap();
        if (GenericUtils.isEmpty(matchKeys)) {
            if (log.isDebugEnabled()) {
                log.debug("authenticate(" + username + ")[" + session + "] no keys");
            }

            return false;
        }
        Collection<? extends PublicKey> keys = matchKeys.get(username);
        if (GenericUtils.isEmpty(keys)){
            return false;
        }else {
            PublicKey matchKey = KeyUtils.findMatchingKey(key, keys);
            boolean matchFound = matchKey != null;
            if (log.isDebugEnabled()) {
                log.debug("authenticate(" + username + ")[" + session + "] match found=" + matchFound);
            }
            return matchFound;
        }
    }
}
