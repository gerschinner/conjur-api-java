package net.conjur.api;

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.StringType;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JacksonInject;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
public class Variable extends Resource {
    // redundant annotations are included to clarify which properties
    // are from json
    @JsonProperty("id")
    private String id;

    @JsonProperty("mime_type")
    private String mimeType;

    @JsonProperty("kind")
    private String kind;

    @JsonProperty("version_count")
    private int versionCount;

    @JsonProperty("ownerid")
    private String ownerId;

    @JsonProperty("userid")
    private String userId;

    @JsonProperty("resource_identifier")
    private String resourceIdentifier;

    private WebTarget target;
    private WebTarget valuesTarget;
    private WebTarget valueTarget;
    private boolean invalidated = false;


    // constructor injects a Resource from which we can initialize our client, auth providers, etc.
    @JsonCreator
    Variable(
            @JacksonInject("resource") final Resource resource,
            @JsonProperty("id") final String id){
        super(resource);
        this.id = id;
        buildTargets();
    }

    public String getId(){
        return id;
    }

    public int getVersionCount(){
        if(invalidated)
            update();
        return versionCount;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getKind() {
        return kind;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getUserId() {
        return userId;
    }

    public String getResourceIdentifier() {
        return resourceIdentifier;
    }

    public <T> T getValue(Class<T> type){
        return valueTarget.request(mimeType).get(type);
    }

    public <T> T getValue(int version, Class<T> type){
        return valueTarget.queryParam("version", String.valueOf(version))
                .request(mimeType).get(type);
    }

    public String getValue(){
        return getValue(String.class);
    }

    public String getValue(int version){
        return getValue(version, String.class);
    }

    public void addValue(String value){
        invalidated = true;
        valuesTarget.request().put(Entity.text(value));
    }

    public <T> void addValue(T entity){
        invalidated = true;
        valuesTarget.request().put(Entity.entity(entity, getMimeType()));
    }

    public void delete(){
        target.request().delete(String.class);
    }

    public boolean exists(){
        try{
            update();
            return true;
        }catch(NotFoundException e){
            return false;
        }catch(ForbiddenException e){
            return true;
        }
    }

    Variable update(){
        final Variable variable = target.request(MediaType.APPLICATION_JSON_TYPE).get(Variable.class);
        mimeType = variable.mimeType;
        kind = variable.kind;
        ownerId = variable.ownerId;
        userId = variable.userId;
        resourceIdentifier = variable.resourceIdentifier;
        versionCount = variable.versionCount;
        invalidated = false;
        return this;
    }

    private void buildTargets(){
        target = target(getEndpoints().getDirectoryUri())
                .path("variables").path(getId());
        valuesTarget = target.path("values");
        valueTarget = target.path("value");
    }
}