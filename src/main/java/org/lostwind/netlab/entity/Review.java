package org.lostwind.netlab.entity;

import lombok.Data;

@Data
public class Review {
    Integer id;
    Integer accountId;
    String username;
    String content;
}
