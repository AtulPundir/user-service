package com.myapp.identityservice.service;

import com.myapp.identityservice.domain.User;
import com.myapp.identityservice.dto.response.BatchResolveResponse;
import com.myapp.identityservice.dto.response.UserDisplayInfo;
import com.myapp.identityservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BatchResolveService {

    private final UserRepository userRepository;
    private final ContactAliasService contactAliasService;

    public BatchResolveService(UserRepository userRepository,
                                ContactAliasService contactAliasService) {
        this.userRepository = userRepository;
        this.contactAliasService = contactAliasService;
    }

    @Transactional(readOnly = true)
    public BatchResolveResponse batchResolve(List<String> userIds, String viewerUserId) {
        List<User> users = userRepository.findByIdIn(userIds);

        Map<String, String> aliases = contactAliasService.getAliasMap(viewerUserId, userIds);

        Map<String, UserDisplayInfo> userMap = users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        user -> new UserDisplayInfo(
                                user.getName(),
                                user.isVerified(),
                                aliases.get(user.getId())
                        )
                ));

        return new BatchResolveResponse(userMap);
    }
}
