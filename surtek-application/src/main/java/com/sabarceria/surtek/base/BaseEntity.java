package com.sabarceria.surtek.base;



import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;

@MappedSuperclass
public class BaseEntity implements Serializable {
    protected String modifiedBy;
    @Temporal(TemporalType.TIMESTAMP)
    protected Date modifiedDate;

    @Column(
            name = "is_deleted"
    )
    protected Boolean deleted = false;
    protected String createdBy;
    @Column(
            updatable = false
    )
    @Temporal(TemporalType.TIMESTAMP)
    protected Date createdDate;

    public Boolean getDeleted() {
        return this.deleted;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public Date getCreatedDate() {
        return this.createdDate;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public BaseEntity() {
    }

    public String getModifiedBy() {
        return this.modifiedBy;
    }

    public Date getModifiedDate() {
        return this.modifiedDate;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

}