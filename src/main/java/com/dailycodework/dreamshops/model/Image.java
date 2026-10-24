package com.dailycodework.dreamshops.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Image {
    /** Largest image accepted, in bytes. */
    public static final int MAX_SIZE = 5 * 1024 * 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fileName;
    private String fileType;

    // A plain byte array maps to bytea on PostgreSQL and a blob type elsewhere, without java.sql.Blob's
    // need for an open connection when the bytes are read.
    @Column(nullable = false, length = MAX_SIZE)
    private byte[] data;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    public Image(String fileName, String fileType, byte[] data, Product product) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.data = data;
        this.product = product;
    }
}
