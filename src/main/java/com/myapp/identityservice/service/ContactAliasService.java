package com.myapp.identityservice.service;

import com.myapp.identityservice.domain.ContactAlias;
import com.myapp.identityservice.dto.request.SetContactAliasRequest;
import com.myapp.identityservice.dto.response.ContactAliasResponse;
import com.myapp.identityservice.exception.NotFoundException;
import com.myapp.identityservice.repository.ContactAliasRepository;
import com.myapp.identityservice.repository.UserRepository;
import com.myapp.identityservice.util.CuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ContactAliasService {

    private static final Logger logger = LoggerFactory.getLogger(ContactAliasService.class);

    private final ContactAliasRepository aliasRepository;
    private final UserRepository userRepository;
    private final CuidGenerator cuidGenerator;

    public ContactAliasService(ContactAliasRepository aliasRepository,
                                UserRepository userRepository,
                                CuidGenerator cuidGenerator) {
        this.aliasRepository = aliasRepository;
        this.userRepository = userRepository;
        this.cuidGenerator = cuidGenerator;
    }

    @Transactional
    public ContactAliasResponse setAlias(String ownerUserId, SetContactAliasRequest request) {
        userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new NotFoundException("Target user not found: " + request.getTargetUserId()));

        return setAliasInternal(ownerUserId, request.getTargetUserId(), request.getAliasName());
    }

    @Transactional
    public ContactAliasResponse setAliasInternal(String ownerUserId, String targetUserId, String aliasName) {
        ContactAlias alias = aliasRepository
                .findByOwnerUserIdAndTargetUserId(ownerUserId, targetUserId)
                .orElse(null);

        if (alias == null) {
            alias = new ContactAlias();
            alias.setId(cuidGenerator.generate());
            alias.setOwnerUserId(ownerUserId);
            alias.setTargetUserId(targetUserId);
        }

        alias.setAliasName(aliasName);
        ContactAlias saved = aliasRepository.save(alias);

        logger.debug("Set alias: owner={}, target={}, alias={}", ownerUserId, targetUserId, aliasName);
        return ContactAliasResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ContactAliasResponse> getAliases(String ownerUserId) {
        return aliasRepository.findByOwnerUserId(ownerUserId)
                .stream()
                .map(ContactAliasResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, String> getAliasMap(String ownerUserId, List<String> targetUserIds) {
        return aliasRepository.findByOwnerUserIdAndTargetUserIdIn(ownerUserId, targetUserIds)
                .stream()
                .collect(Collectors.toMap(
                        ContactAlias::getTargetUserId,
                        ContactAlias::getAliasName
                ));
    }
}
