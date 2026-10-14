package com.sabarceria.surtek.oauth2;

import java.util.Map;

public class DiscordOAuth2UserInfo extends OAuth2UserInfo {

    public DiscordOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        Object id = attributes.get("id");
        return id != null ? String.valueOf(id) : null;
    }

    @Override
    public String getName() {
        String globalName = (String) attributes.get("global_name");
        return globalName != null && !globalName.isBlank() ? globalName : (String) attributes.get("username");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getAvatarUrl() {
        String avatar = (String) attributes.get("avatar");
        String id = getId();
        if (avatar != null && id != null) {
            return "https://cdn.discordapp.com/avatars/" + id + "/" + avatar + ".png";
        }
        return null;
    }
}
