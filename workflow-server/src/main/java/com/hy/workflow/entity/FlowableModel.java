package com.hy.workflow.entity;


import com.hy.workflow.common.base.BaseEntity;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;


@Entity
@Table
public class FlowableModel extends BaseEntity {

    @Id
    @GenericGenerator(name="idGenerator",strategy = "org.hibernate.id.UUIDGenerator")
    @GeneratedValue(generator="idGenerator")
    private String id;

    private String name;

    private String modelKey;

    private String description;

    private int version;

    private String tenantId;

    @Lob
    @Basic(fetch=FetchType.LAZY)
    private String xml;

    @Lob
    @Basic(fetch=FetchType.LAZY)
    private String svg;

    private String createUser;

    private String lastUpdatedUser;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModelKey() {
        return modelKey;
    }

    public void setModelKey(String modelKey) {
        this.modelKey = modelKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public String getSvg() {
        return svg;
    }

    public void setSvg(String svg) {
        this.svg = svg;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public String getLastUpdatedUser() {
        return lastUpdatedUser;
    }

    public void setLastUpdatedUser(String lastUpdatedUser) {
        this.lastUpdatedUser = lastUpdatedUser;
    }


}
