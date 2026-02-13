package cc.ivera.service;

import cc.ivera.api.dto.CreateUserRequest;
import cc.ivera.api.dto.UpdateUserRequest;
import cc.ivera.api.dto.UserPageQuery;
import cc.ivera.api.dto.UserResponse;
import cc.ivera.common.ApiException;
import cc.ivera.common.ErrorCodes;
import cc.ivera.common.PageResponse;
import cc.ivera.config.PagingProperties;
import cc.ivera.mongo.UserDoc;
import cc.ivera.mongo.UserRepository;
import cc.ivera.util.SortParser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repo;
    private final MongoTemplate mongoTemplate;
    private final PagingProperties pagingProps;

    private static final Set<String> ALLOWED_SORT_FIELDS = new HashSet<>(Arrays.asList(
            "createdAt", "updatedAt", "uid", "email", "phone", "nickname", "status"
    ));

    public UserResponse create(CreateUserRequest req) {
        if (repo.existsByUid(req.getUid())) {
            throw new ApiException(ErrorCodes.CONFLICT, "uid already exists: " + req.getUid());
        }
        Instant now = Instant.now();
        UserDoc doc = UserDoc.builder()
                .uid(req.getUid())
                .phone(req.getPhone())
                .email(req.getEmail())
                .nickname(req.getNickname())
                .status(req.getStatus() == null ? 1 : req.getStatus())
                .createdAt(now)
                .updatedAt(now)
                .build();
        doc = repo.save(doc);
        return toResp(doc);
    }

    public UserResponse get(String id) {
        UserDoc doc = repo.findById(id).orElseThrow(() -> new ApiException(ErrorCodes.NOT_FOUND, "not found: " + id));
        return toResp(doc);
    }

    public UserResponse update(String id, UpdateUserRequest req) {
        UserDoc doc = repo.findById(id).orElseThrow(() -> new ApiException(ErrorCodes.NOT_FOUND, "not found: " + id));
        if (req.getPhone() != null) doc.setPhone(req.getPhone());
        if (req.getEmail() != null) doc.setEmail(req.getEmail());
        if (req.getNickname() != null) doc.setNickname(req.getNickname());
        if (req.getStatus() != null) doc.setStatus(req.getStatus());
        doc.setUpdatedAt(Instant.now());
        doc = repo.save(doc);
        return toResp(doc);
    }

    public void delete(String id) {
        if (!repo.existsById(id)) return;
        repo.deleteById(id);
    }

    /**
     * Production-grade paging:
     * - Dynamic criteria via MongoTemplate
     * - Safe sort whitelist
     * - Hard cap for page size
     * - Keyword regex is escaped to prevent ReDoS / injection
     */
    public PageResponse<UserResponse> page(UserPageQuery q) {
        int page = q.getPage() == null ? 1 : q.getPage();
        int size = q.getSize() == null ? pagingProps.getDefaultPageSize() : q.getSize();
        size = Math.min(size, pagingProps.getMaxPageSize());
        if (page < 1) page = 1;
        if (size <= 0) size = pagingProps.getDefaultPageSize();

        Sort defaultSort = Sort.by(Sort.Direction.DESC, "updatedAt");
        Sort sort = SortParser.parse(q.getSort(), ALLOWED_SORT_FIELDS, defaultSort);

        Criteria criteria = new Criteria();
        List<Criteria> and = new ArrayList<>();

        if (StringUtils.hasText(q.getUid())) {
            and.add(Criteria.where("uid").is(q.getUid().trim()));
        }
        if (StringUtils.hasText(q.getPhone())) {
            and.add(Criteria.where("phone").is(q.getPhone().trim()));
        }
        if (StringUtils.hasText(q.getEmail())) {
            // case-insensitive exact match for email
            String email = q.getEmail().trim();
            and.add(Criteria.where("email").regex("^" + Pattern.quote(email) + "$", "i"));
        }
        if (StringUtils.hasText(q.getNickname())) {
            and.add(Criteria.where("nickname").regex(containsRegex(q.getNickname().trim()), "i"));
        }

        // status=1,0
        if (StringUtils.hasText(q.getStatus())) {
            List<Integer> statuses = parseInts(q.getStatus());
            if (!statuses.isEmpty()) {
                and.add(Criteria.where("status").in(statuses));
            }
        }

        // time ranges
        addRange(and, "createdAt", q.getCreatedFrom(), q.getCreatedTo());
        addRange(and, "updatedAt", q.getUpdatedFrom(), q.getUpdatedTo());

        // keyword across multiple fields
        if (StringUtils.hasText(q.getKeyword())) {
            String rx = containsRegex(q.getKeyword().trim());
            List<Criteria> or = new ArrayList<>();
            or.add(Criteria.where("uid").regex(rx, "i"));
            or.add(Criteria.where("phone").regex(rx));
            or.add(Criteria.where("email").regex(rx, "i"));
            or.add(Criteria.where("nickname").regex(rx, "i"));
            and.add(new Criteria().orOperator(or.toArray(new Criteria[0])));
        }

        if (!and.isEmpty()) {
            criteria = new Criteria().andOperator(and.toArray(new Criteria[0]));
        }

        Query query = new Query(criteria).with(sort);
        long total = mongoTemplate.count(query, UserDoc.class);

        query.skip((long) (page-1) * size).limit(size);

        List<UserDoc> docs = mongoTemplate.find(query, UserDoc.class);
        List<UserResponse> items = docs.stream().map(this::toResp).collect(Collectors.toList());

        return new PageResponse<>(page, size, total, items);
    }

    private void addRange(List<Criteria> and, String field, Instant from, Instant to) {
        if (from == null && to == null) return;
        Criteria c = Criteria.where(field);
        if (from != null) c = c.gte(from);
        if (to != null) c = c.lte(to);
        and.add(c);
    }

    private List<Integer> parseInts(String s) {
        try {
            return Arrays.stream(s.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ApiException(ErrorCodes.BAD_REQUEST, "invalid status param: " + s);
        }
    }

    private String containsRegex(String keyword) {
        // escape regex metacharacters to prevent injection / heavy regex
        return ".*" + Pattern.quote(keyword) + ".*";
    }

    private UserResponse toResp(UserDoc d) {
        return UserResponse.builder()
                .id(d.getId())
                .uid(d.getUid())
                .phone(d.getPhone())
                .email(d.getEmail())
                .nickname(d.getNickname())
                .status(d.getStatus())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
