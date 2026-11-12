package com.petfeed.comment.service;

import com.petfeed.comment.domain.CommentThread;
import com.petfeed.comment.repository.CommentThreadRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class CommentThreadInitializer {
    private final CommentThreadRepository threads;
    private final TransactionTemplate transaction;

    public CommentThreadInitializer(CommentThreadRepository threads, PlatformTransactionManager manager) {
        this.threads = threads;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void ensure(Long postId) {
        try {
            transaction.executeWithoutResult(status -> {
                if (!threads.existsById(postId)) threads.saveAndFlush(new CommentThread(postId));
            });
        } catch (DataIntegrityViolationException conflict) {
            // A concurrent request may have initialized the same post. Its committed row is used below.
            if (!threads.existsById(postId)) throw conflict;
        }
    }
}
