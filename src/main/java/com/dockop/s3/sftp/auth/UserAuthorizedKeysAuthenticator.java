package com.dockop.s3.sftp.auth;

import com.dockop.s3.sftp.common.Constants;
import com.dockop.s3.sftp.utils.ResolveAuthorizedUtils;
import org.apache.sshd.common.config.keys.AuthorizedKeyEntry;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.config.keys.PublicKeyEntryResolver;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.io.IoUtils;
import org.apache.sshd.common.util.io.ModifiableFileWatcher;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.auth.pubkey.RejectAllPublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 类名称: UserAuthorizedKeysAuthenticator <br>
 * 类描述: <br>
 *
 * @author Tis
 * @version 1.0.0
 * @since 2019/3/20 下午12:59
 */
public class UserAuthorizedKeysAuthenticator extends ModifiableFileWatcher implements PublickeyAuthenticator {

    private static Logger logger = LoggerFactory.getLogger(UserAuthorizedKeysAuthenticator.class);

    private UserManager userManager;

    public static final String STD_AUTHORIZED_KEYS_FILENAME = "authorized_keys";

    private static final class LazyDefaultAuthorizedKeysFileHolder {
        private static final Path KEYS_FILE = PublicKeyEntry.getDefaultKeysFolderPath().resolve(STD_AUTHORIZED_KEYS_FILENAME);
    }

    private final AtomicReference<PublickeyAuthenticator> delegateHolder =  // assumes initially reject-all
            new AtomicReference<>(RejectAllPublickeyAuthenticator.INSTANCE);

    public UserAuthorizedKeysAuthenticator(UserManager userManager) {
        this(Objects.requireNonNull(getDefaultAuthorizedKeysFile(), "No file to watch"),userManager);
    }

    public UserAuthorizedKeysAuthenticator(Path file, UserManager userManager) {
        this(file, userManager, IoUtils.EMPTY_LINK_OPTIONS);
    }

    public UserAuthorizedKeysAuthenticator(Path file, UserManager userManager,LinkOption... options) {
        super(file, options);
        this.userManager = userManager;
    }

    public UserAuthorizedKeysAuthenticator(File file, UserManager userManager) {
        super(file);
        this.userManager = userManager;
    }

    @Override
    public boolean authenticate(String username, PublicKey key, ServerSession session) {
        if (!isValidUsername(username, session)) {
            if (log.isDebugEnabled()) {
                log.debug("authenticate(" + username + ")[" + session + "][" + key.getAlgorithm() + "] invalid user name - file = " + getPath());
            }
            return false;
        }

        try {
            PublickeyAuthenticator delegate =
                    Objects.requireNonNull(resolvePublickeyAuthenticator(username, session), "No delegate");
            boolean accepted = delegate.authenticate(username, key, session);
            if (log.isDebugEnabled()) {
                log.debug("authenticate(" + username + ")[" + session + "][" + key.getAlgorithm() + "] accepted " + accepted + " from " + getPath());
            }
            if (accepted){
                try {
                    User user = userManager.getUserByName(username);
                    session.setAttribute(Constants.USER_SESSION_KEY, user);
                    session.setAttribute(Constants.USE_S3, user.isUseS3());
                } catch (Throwable e) {
                    logger.error("getUserByName exception", e);
                }
            }

            return accepted;
        } catch (Throwable e) {
            if (log.isDebugEnabled()) {
                log.debug("authenticate(" + username + ")[" + session + "][" + getPath() + "]"
                        + " failed (" + e.getClass().getSimpleName() + ")"
                        + " to resolve delegate: " + e.getMessage());
            }

            if (log.isTraceEnabled()) {
                log.trace("authenticate(" + username + ")[" + session + "][" + getPath() + "] failure details", e);
            }
            return false;
        }
    }


    protected boolean isValidUsername(String username, ServerSession session) {
        return GenericUtils.isNotEmpty(username);
    }

    protected PublickeyAuthenticator resolvePublickeyAuthenticator(String username, ServerSession session)
            throws IOException, GeneralSecurityException {
        if (checkReloadRequired()) {
            /* Start fresh - NOTE: if there is any error then we want to reject all attempts
             * since we don't want to remain with the previous data - safer that way
             */
            delegateHolder.set(RejectAllPublickeyAuthenticator.INSTANCE);

            Path path = getPath();
            if (exists()) {
                Collection<AuthorizedKeyEntry> entries = reloadAuthorizedKeys(path, username, session);
                if (GenericUtils.size(entries) > 0) {
                    delegateHolder.set(new UserMappingPublickeyAuthenticator(ResolveAuthorizedUtils.resolveAuthorizedKeys(getFallbackPublicKeyEntryResolver(),entries)));
                }
            } else {
                log.info("resolvePublickeyAuthenticator(" + username + ")[" + session + "] no authorized keys file at " + path);
            }
        }

        return delegateHolder.get();
    }

    protected PublicKeyEntryResolver getFallbackPublicKeyEntryResolver() {
        return PublicKeyEntryResolver.IGNORING;
    }

    protected Collection<AuthorizedKeyEntry> reloadAuthorizedKeys(Path path, String username, ServerSession session) throws IOException {
        Collection<AuthorizedKeyEntry> entries = AuthorizedKeyEntry.readAuthorizedKeys(path);
        log.info("reloadAuthorizedKeys(" + username + ")[" + session + "] loaded " + GenericUtils.size(entries) + " keys from " + path);
        updateReloadAttributes();
        return entries;
    }

    /**
     * @return The default {@link Path} location of the OpenSSH authorized keys file
     */
    @SuppressWarnings("synthetic-access")
    public static Path getDefaultAuthorizedKeysFile() {
        return LazyDefaultAuthorizedKeysFileHolder.KEYS_FILE;
    }

}
