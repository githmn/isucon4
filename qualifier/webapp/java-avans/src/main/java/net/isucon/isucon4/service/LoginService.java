/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Manabu Matsuzaki
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.isucon.isucon4.service;

import com.google.common.primitives.Bytes;
import net.isucon.isucon4.constant.IsuconConstant;
import net.isucon.isucon4.database.Transaction;
import net.isucon.isucon4.exception.BusinessCommitException;
import net.isucon.isucon4.exception.BusinessException;
import net.isucon.isucon4.repository.LoggingRepository;
import net.isucon.isucon4.repository.LoginRepository;
import net.isucon.isucon4.row.LoginLog;
import net.isucon.isucon4.row.User;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

@Singleton
public class LoginService {

    @Inject
    Transaction transaction;

    @Inject
    LoginRepository loginRepository;

    @Inject
    LoggingRepository loggingRepository;

    public LoginLog attemptLogin(String login, String password, String ip) {

        return transaction.run(() -> {
            User user = loginRepository.findUserByLogin(login)
                    .orElseThrow(() -> new BusinessCommitException("Wrong username or password"));

            try {

                checkIpBanned(ip);
                checkUserLocked(user);

                String hash = calculatePasswordHash(password, user.getSalt());
                if (Objects.equals(user.getPasswordHash(), hash)) {
                    loggingRepository.create(true, login, ip, user);
                    return loginRepository.findLoginLogByUserId(user.getId());
                } else {
                    throw new BusinessCommitException("Wrong username or password");
                }
            } catch (BusinessException e) {
                loggingRepository.create(false, login, ip, user);
                throw e;
            }
        });
    }

    void checkIpBanned(String ip) {
        long failures = loginRepository.countBannedIp(ip);
        if (failures >= IsuconConstant.THRESHOLD_IP_BAN) {
            throw new BusinessCommitException("You're banned.");
        }
    }

    void checkUserLocked(User user) {
        long failures = loginRepository.countLockedUser(user.getId());
        if (failures >= IsuconConstant.THRESHOLD_USER_LOCK) {
            throw new BusinessCommitException("This account is locked.");
        }
    }

    String calculatePasswordHash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.reset();
            String v = String.format("%s:%s", password, salt);
            md.update(v.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();

            StringBuilder sb = Bytes.asList(digest).stream()
                    .map(b -> String.format("%02x", b))
                    .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append);

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
