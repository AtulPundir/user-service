package com.myapp.userservice.dto.response;

import java.util.ArrayList;
import java.util.List;

public class BulkAddUsersResponse {

    private int added;
    private int alreadyMembers;
    private int created;
    private Details details;

    public BulkAddUsersResponse() {
        this.details = new Details();
    }

    public int getAdded() {
        return added;
    }

    public void setAdded(int added) {
        this.added = added;
    }

    public int getAlreadyMembers() {
        return alreadyMembers;
    }

    public void setAlreadyMembers(int alreadyMembers) {
        this.alreadyMembers = alreadyMembers;
    }

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public Details getDetails() {
        return details;
    }

    public void setDetails(Details details) {
        this.details = details;
    }

    public void addAddedUser(UserResponse user, MembershipResponse membership) {
        this.added++;
        this.details.added.add(new AddedDetail(user, membership));
    }

    public void addAlreadyMember(UserResponse user, String message) {
        this.alreadyMembers++;
        this.details.alreadyMembers.add(new AlreadyMemberDetail(user, message));
    }

    public void addCreatedUser(UserResponse user, boolean isVerified) {
        this.created++;
        this.details.created.add(new CreatedDetail(user, isVerified));
    }

    public static class Details {
        private List<AddedDetail> added = new ArrayList<>();
        private List<AlreadyMemberDetail> alreadyMembers = new ArrayList<>();
        private List<CreatedDetail> created = new ArrayList<>();

        public List<AddedDetail> getAdded() {
            return added;
        }

        public void setAdded(List<AddedDetail> added) {
            this.added = added;
        }

        public List<AlreadyMemberDetail> getAlreadyMembers() {
            return alreadyMembers;
        }

        public void setAlreadyMembers(List<AlreadyMemberDetail> alreadyMembers) {
            this.alreadyMembers = alreadyMembers;
        }

        public List<CreatedDetail> getCreated() {
            return created;
        }

        public void setCreated(List<CreatedDetail> created) {
            this.created = created;
        }
    }

    public static class AddedDetail {
        private UserResponse user;
        private MembershipResponse membership;

        public AddedDetail() {
        }

        public AddedDetail(UserResponse user, MembershipResponse membership) {
            this.user = user;
            this.membership = membership;
        }

        public UserResponse getUser() {
            return user;
        }

        public void setUser(UserResponse user) {
            this.user = user;
        }

        public MembershipResponse getMembership() {
            return membership;
        }

        public void setMembership(MembershipResponse membership) {
            this.membership = membership;
        }
    }

    public static class AlreadyMemberDetail {
        private UserResponse user;
        private String message;

        public AlreadyMemberDetail() {
        }

        public AlreadyMemberDetail(UserResponse user, String message) {
            this.user = user;
            this.message = message;
        }

        public UserResponse getUser() {
            return user;
        }

        public void setUser(UserResponse user) {
            this.user = user;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class CreatedDetail {
        private UserResponse user;
        private boolean isVerified;

        public CreatedDetail() {
        }

        public CreatedDetail(UserResponse user, boolean isVerified) {
            this.user = user;
            this.isVerified = isVerified;
        }

        public UserResponse getUser() {
            return user;
        }

        public void setUser(UserResponse user) {
            this.user = user;
        }

        public boolean isVerified() {
            return isVerified;
        }

        public void setVerified(boolean verified) {
            isVerified = verified;
        }
    }
}
