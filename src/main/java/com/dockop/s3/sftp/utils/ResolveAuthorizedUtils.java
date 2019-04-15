package com.dockop.s3.sftp.utils;

import org.apache.sshd.common.config.keys.AuthorizedKeyEntry;
import org.apache.sshd.common.config.keys.PublicKeyEntryResolver;
import org.apache.sshd.common.util.GenericUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.*;

/**
 * 类名称: ResolveAuthorizedUtils <br>
 * 类描述: 解析出和用户对应的PublicKey<br>
 *
 * @author qun.huang
 * @version 1.0.0
 * @since 2019/3/22 下午6:26
 */
public class ResolveAuthorizedUtils {

    public static Map<String,Collection<? extends PublicKey>> resolveAuthorizedKeys(PublicKeyEntryResolver fallbackResolver, Collection<? extends AuthorizedKeyEntry> entries)
            throws IOException, GeneralSecurityException {
        if (GenericUtils.isEmpty(entries)) {
            return Collections.emptyMap();
        }
        HashMap<String, Collection<? extends PublicKey>> keys = new HashMap<>(entries.size());
        for (AuthorizedKeyEntry e : entries) {
            PublicKey k = e.resolvePublicKey(fallbackResolver);
            if (k != null) {
                int ind = e.getComment().indexOf('@');
                if (ind != -1){
                    String user = e.getComment().substring(0, ind);
                    if(GenericUtils.isEmpty(keys.get(user))){
                        List<PublicKey> keyList = new ArrayList<>();
                        keyList.add(k);
                        keys.put(user,keyList);
                    }else {
                        List<PublicKey> keyList = (List<PublicKey>) keys.get(user);
                        keyList.add(k);
                    }
                }
            }
        }
        return keys;
    }

}
