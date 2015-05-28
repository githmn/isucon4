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

package net.isucon.isucon4.database;

import lombok.extern.slf4j.Slf4j;
import me.geso.tinyorm.TinyORM;
import net.isucon.isucon4.exception.BusinessCommitException;
import net.isucon.isucon4.exception.BusinessException;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Supplier;

@Slf4j
public class Transaction {

    @Inject
    TinyORM orm;

    public <T> T run(Supplier<T> block) {

        Objects.requireNonNull(orm);

        Connection connection = orm.getConnection();

        Objects.requireNonNull(connection);

        Boolean autoCommit = null;

        try {
            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            T ret = block.get();

            connection.commit();

            return ret;
        } catch (Exception e) {
            try {
                if (e instanceof BusinessCommitException) {
                    connection.commit();
                    throw e;
                } else {
                    connection.rollback();
                    if (e instanceof BusinessException) {
                        throw e;
                    } else {
                        log.error("try block error!", e);
                        throw new RuntimeException(e);
                    }
                }
            } catch (SQLException e1) {
                log.error("Transaction (commit|rollback) error!", e);
                throw new RuntimeException("Transaction (commit|rollback) error!", e1);
            }
        } finally {
            try {
                if (autoCommit != null) {
                    connection.setAutoCommit(autoCommit);
                }
            } catch (SQLException e) {
                log.error("setAutoCommit error!", e);
            }
        }
    }
}
