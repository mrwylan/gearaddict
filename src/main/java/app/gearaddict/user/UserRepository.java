package app.gearaddict.user;

import app.gearaddict.jooq.tables.records.UsersRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static app.gearaddict.jooq.Tables.USERS;
import static org.jooq.impl.DSL.lower;

@Repository
public class UserRepository {

    private final DSLContext dsl;

    public UserRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public boolean existsByUsernameIgnoreCase(String username) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(USERS)
                        .where(lower(USERS.USERNAME).eq(username.toLowerCase())));
    }

    public boolean existsByUsernameIgnoreCaseExcludingId(String username, Long excludeId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(USERS)
                        .where(lower(USERS.USERNAME).eq(username.toLowerCase()))
                        .and(USERS.ID.ne(excludeId)));
    }

    public Optional<UsersRecord> findById(Long id) {
        return dsl.selectFrom(USERS)
                .where(USERS.ID.eq(id))
                .fetchOptional();
    }

    public UsersRecord updateProfile(Long id, String username, String bio) {
        return dsl.update(USERS)
                .set(USERS.USERNAME, username)
                .set(USERS.BIO, bio)
                .where(USERS.ID.eq(id))
                .returning()
                .fetchOne();
    }

    public UsersRecord updateInventoryVisibility(Long id, boolean publicInventory) {
        return dsl.update(USERS)
                .set(USERS.PUBLIC_INVENTORY, publicInventory)
                .where(USERS.ID.eq(id))
                .returning()
                .fetchOne();
    }

    public boolean existsByEmail(String email) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(USERS)
                        .where(USERS.EMAIL.eq(email)));
    }

    public Optional<UsersRecord> findByEmail(String email) {
        return dsl.selectFrom(USERS)
                .where(USERS.EMAIL.eq(email))
                .fetchOptional();
    }

    public Optional<UsersRecord> findByUsername(String username) {
        return dsl.selectFrom(USERS)
                .where(USERS.USERNAME.eq(username))
                .fetchOptional();
    }

    public UsersRecord insert(String username, String email, String passwordHash) {
        return dsl.insertInto(USERS)
                .set(USERS.USERNAME, username)
                .set(USERS.EMAIL, email)
                .set(USERS.PASSWORD, passwordHash)
                .set(USERS.PUBLIC_INVENTORY, false)
                .returning()
                .fetchOne();
    }

    public void deleteAll() {
        dsl.deleteFrom(USERS).execute();
    }
}
