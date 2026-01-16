package com.example.jobplatform.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cvs")
public class CV {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;
    private String fileType;


    @Basic(fetch = FetchType.LAZY)
    @Column(name = "data", columnDefinition = "bytea")
    private byte[] data;

    // Собственик на CV-то
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;


    public CV() {}
    public CV(String filename, String fileType, byte[] data) {
        this.filename = filename;
        this.fileType = fileType;
        this.data = data;
    }

    public Long getId() { return id; }
    public String getFilename() { return filename; }
    public String getFileType() { return fileType; }
    public byte[] getData() { return data; }
    public User getOwner() { return owner; }

    public void setId(Long id) { this.id = id; }
    public void setFilename(String filename) { this.filename = filename; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public void setData(byte[] data) { this.data = data; }
    public void setOwner(User owner) { this.owner = owner; }
}