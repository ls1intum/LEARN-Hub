import React, { useState, useEffect, useCallback } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { apiService } from "@/services/apiService";
import type { User as UserType } from "@/types/activity";
import { logger } from "@/services/logger";

// Use User type from activity types

interface CreateUserData {
  email: string;
  first_name: string;
  last_name: string;
  role: string;
  password: string;
}

interface EditUserData {
  email: string;
  first_name: string;
  last_name: string;
  role: string;
  password: string;
}

export const UserManagementPage: React.FC = () => {
  const [error, setError] = useState<string | null>(null);
  const [users, setUsers] = useState<UserType[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [isEditing, setIsEditing] = useState<number | null>(null);
  const [message, setMessage] = useState("");

  // Create user form state
  const [createForm, setCreateForm] = useState<CreateUserData>({
    email: "",
    first_name: "",
    last_name: "",
    role: "TEACHER",
    password: "",
  });

  // Edit user form state
  const [editForm, setEditForm] = useState<EditUserData>({
    email: "",
    first_name: "",
    last_name: "",
    role: "TEACHER",
    password: "",
  });

  const fetchUsers = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const response = await apiService.getUsers();
      if (response) {
        setUsers(response.users);
      }
    } catch (err) {
      logger.error("Error fetching users", err, "UserManagementPage");
      setError(err instanceof Error ? err.message : "Failed to load users");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsCreating(true);
    setMessage("");
    setError(null);

    try {
      // Include all required fields for user creation
      const submitData = {
        email: createForm.email,
        first_name: createForm.first_name,
        last_name: createForm.last_name,
        role: createForm.role as "ADMIN" | "TEACHER",
        password: createForm.password,
      };

      await apiService.createUser(submitData);
      setMessage("User created successfully");
      setCreateForm({
        email: "",
        first_name: "",
        last_name: "",
        role: "TEACHER",
        password: "",
      });
      fetchUsers(); // Refresh the list
    } catch (err) {
      logger.error("Error creating user", err, "UserManagementPage");
      setError(err instanceof Error ? err.message : "Failed to create user");
    } finally {
      setIsCreating(false);
    }
  };

  const handleEditUser = async (userId: number, e: React.FormEvent) => {
    e.preventDefault();
    setMessage("");
    setError(null);

    try {
      const updateData: Partial<typeof editForm> = {
        email: editForm.email,
        first_name: editForm.first_name,
        last_name: editForm.last_name,
        role: editForm.role as "ADMIN" | "TEACHER",
      };

      // Include password only if provided
      if (editForm.password) {
        updateData.password = editForm.password;
      }

      await apiService.updateUser(
        userId,
        updateData as import("@/types/api").UserRequest,
      );
      setMessage("User updated successfully");
      setIsEditing(null);
      setEditForm({
        email: "",
        first_name: "",
        last_name: "",
        role: "TEACHER",
        password: "",
      });
      fetchUsers(); // Refresh the list
    } catch (err) {
      logger.error("Error updating user", err, "UserManagementPage");
      setError(err instanceof Error ? err.message : "Failed to update user");
    }
  };

  const handleDeleteUser = async (userId: number) => {
    if (!window.confirm("Are you sure you want to delete this user?")) {
      return;
    }

    setMessage("");
    setError(null);

    try {
      await apiService.deleteUser(userId);
      setMessage("User deleted successfully");
      fetchUsers(); // Refresh the list
    } catch (err) {
      logger.error("Error deleting user", err, "UserManagementPage");
      setError(err instanceof Error ? err.message : "Failed to delete user");
    }
  };

  const startEdit = (user: UserType) => {
    setEditForm({
      email: user.email,
      first_name: user.first_name || "",
      last_name: user.last_name || "",
      role: user.role,
      password: "",
    });
    setIsEditing(user.id);
  };

  const cancelEdit = () => {
    setIsEditing(null);
    setEditForm({
      email: "",
      first_name: "",
      last_name: "",
      role: "TEACHER",
      password: "",
    });
  };

  if (isLoading) {
    return (
      <div className="w-full">
        <div className="text-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-muted-foreground">Loading user management...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full">
      <div className="mb-8">
        <h2 className="text-2xl font-bold text-foreground mb-2">
          User Management
        </h2>
        <p className="text-lg text-muted-foreground">
          Manage users and their roles in the system.
        </p>
      </div>

      <div className="bg-card p-6 rounded-lg shadow-sm border border-border max-w-6xl">
        <div className="text-center mb-6">
          <h1 className="text-2xl sm:text-3xl font-semibold mb-2">
            User Management
          </h1>
          <p className="text-sm text-muted-foreground">
            Manage system users and their roles.
          </p>
        </div>

        {/* Messages */}
        {message && (
          <div className="mb-4 p-3 bg-success/10 border border-success text-success rounded">
            {message}
          </div>
        )}
        {error && (
          <div className="mb-4 p-4 bg-destructive/10 border border-destructive/20 rounded-lg">
            <p className="text-destructive text-sm">{error}</p>
          </div>
        )}

        {/* Create User Form */}
        <div className="mb-8 p-4 bg-muted/10 rounded-lg">
          <h2 className="text-lg font-semibold mb-4">Create New User</h2>
          <form onSubmit={handleCreateUser} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div>
                <Label htmlFor="create-email">Email *</Label>
                <Input
                  id="create-email"
                  type="email"
                  value={createForm.email}
                  onChange={(e) =>
                    setCreateForm((prev) => ({
                      ...prev,
                      email: e.target.value,
                    }))
                  }
                  placeholder="user@example.com"
                  required
                  className="mt-1"
                />
              </div>
              <div>
                <Label htmlFor="create-first-name">First Name *</Label>
                <Input
                  id="create-first-name"
                  type="text"
                  value={createForm.first_name}
                  onChange={(e) =>
                    setCreateForm((prev) => ({
                      ...prev,
                      first_name: e.target.value,
                    }))
                  }
                  placeholder="John"
                  required
                  className="mt-1"
                />
              </div>
              <div>
                <Label htmlFor="create-last-name">Last Name *</Label>
                <Input
                  id="create-last-name"
                  type="text"
                  value={createForm.last_name}
                  onChange={(e) =>
                    setCreateForm((prev) => ({
                      ...prev,
                      last_name: e.target.value,
                    }))
                  }
                  placeholder="Doe"
                  required
                  className="mt-1"
                />
              </div>
              <div>
                <Label htmlFor="create-role">Role *</Label>
                <Select
                  value={createForm.role}
                  onValueChange={(value: "TEACHER" | "ADMIN") =>
                    setCreateForm((prev) => ({ ...prev, role: value }))
                  }
                >
                  <SelectTrigger className="mt-1">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="TEACHER">Teacher</SelectItem>
                    <SelectItem value="ADMIN">Admin</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="lg:col-span-2">
                <Label htmlFor="create-password">Password *</Label>
                <Input
                  id="create-password"
                  type="password"
                  value={createForm.password}
                  onChange={(e) =>
                    setCreateForm((prev) => ({
                      ...prev,
                      password: e.target.value,
                    }))
                  }
                  placeholder="Enter password for user"
                  required={true}
                  className="mt-1"
                />
                <p className="text-xs text-muted-foreground mt-1">
                  {createForm.role === "ADMIN"
                    ? "Admin users will use this password to login"
                    : "Teacher users will receive this password via email and can use it to login"}
                </p>
                <p
                  className={`text-xs mt-1 ${createForm.password.length < 8 ? "text-orange-600" : "text-green-600"}`}
                >
                  Password length: {createForm.password.length}/8 characters
                  minimum
                </p>
              </div>
            </div>
            <Button
              type="submit"
              className="bg-green-600 hover:bg-green-700"
              disabled={isCreating}
            >
              {isCreating ? "Creating..." : "Create User"}
            </Button>
          </form>
        </div>

        {/* Users List */}
        <div>
          <h2 className="text-lg font-semibold mb-4">Existing Users</h2>
          <div className="overflow-x-auto">
            <table className="w-full border-collapse border border-border">
              <thead>
                <tr className="bg-muted/20">
                  <th className="border border-border px-4 py-2 text-left">
                    ID
                  </th>
                  <th className="border border-border px-4 py-2 text-left">
                    Email
                  </th>
                  <th className="border border-border px-4 py-2 text-left">
                    First Name
                  </th>
                  <th className="border border-border px-4 py-2 text-left">
                    Last Name
                  </th>
                  <th className="border border-border px-4 py-2 text-left">
                    Role
                  </th>
                  <th className="border border-border px-4 py-2 text-left">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody>
                {users && users.length > 0 ? (
                  users.map((user) => (
                    <tr key={user.id} className="hover:bg-muted/10">
                      <td className="border border-border px-4 py-2">
                        {user.id}
                      </td>
                      <td className="border border-border px-4 py-2">
                        {isEditing === user.id ? (
                          <Input
                            value={editForm.email}
                            onChange={(e) =>
                              setEditForm((prev) => ({
                                ...prev,
                                email: e.target.value,
                              }))
                            }
                            className="w-full"
                          />
                        ) : (
                          user.email
                        )}
                      </td>
                      <td className="border border-border px-4 py-2">
                        {isEditing === user.id ? (
                          <Input
                            value={editForm.first_name}
                            onChange={(e) =>
                              setEditForm((prev) => ({
                                ...prev,
                                first_name: e.target.value,
                              }))
                            }
                            className="w-full"
                          />
                        ) : (
                          user.first_name || "-"
                        )}
                      </td>
                      <td className="border border-border px-4 py-2">
                        {isEditing === user.id ? (
                          <Input
                            value={editForm.last_name}
                            onChange={(e) =>
                              setEditForm((prev) => ({
                                ...prev,
                                last_name: e.target.value,
                              }))
                            }
                            className="w-full"
                          />
                        ) : (
                          user.last_name || "-"
                        )}
                      </td>
                      <td className="border border-border px-4 py-2">
                        {isEditing === user.id ? (
                          <Select
                            value={editForm.role}
                            onValueChange={(value: "TEACHER" | "ADMIN") =>
                              setEditForm((prev) => ({
                                ...prev,
                                role: value,
                              }))
                            }
                          >
                            <SelectTrigger className="w-full">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              <SelectItem value="TEACHER">Teacher</SelectItem>
                              <SelectItem value="ADMIN">Admin</SelectItem>
                            </SelectContent>
                          </Select>
                        ) : (
                          <span
                            className={`px-2 py-1 rounded text-xs font-medium ${
                              user.role === "ADMIN"
                                ? "bg-destructive/10 text-destructive"
                                : "bg-primary/10 text-primary"
                            }`}
                          >
                            {user.role}
                          </span>
                        )}
                      </td>
                      <td className="border border-border px-4 py-2">
                        {isEditing === user.id ? (
                          <div className="flex gap-2">
                            <Button
                              onClick={(e) => handleEditUser(user.id, e)}
                              size="sm"
                              className="bg-green-600 hover:bg-green-700"
                            >
                              Save
                            </Button>
                            <Button
                              onClick={cancelEdit}
                              size="sm"
                              variant="outline"
                            >
                              Cancel
                            </Button>
                          </div>
                        ) : (
                          <div className="flex gap-2">
                            <Button
                              onClick={() => startEdit(user)}
                              size="sm"
                              variant="outline"
                            >
                              Edit
                            </Button>
                            <Button
                              onClick={() => handleDeleteUser(user.id)}
                              size="sm"
                              className="bg-destructive hover:bg-destructive/80"
                            >
                              Delete
                            </Button>
                          </div>
                        )}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td
                      colSpan={6}
                      className="border border-border px-4 py-8 text-center text-muted-foreground"
                    >
                      No users found
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {/* Edit Password Section */}
          {isEditing && (
            <div className="mt-4 p-4 bg-yellow-50 rounded-lg">
              <h3 className="text-md font-semibold mb-2">
                Change Password{" "}
                {editForm.role === "TEACHER" ? "(Not Available)" : "(Optional)"}
              </h3>
              <div className="flex gap-2">
                <Input
                  type="password"
                  value={editForm.password}
                  onChange={(e) =>
                    setEditForm((prev) => ({
                      ...prev,
                      password: e.target.value,
                    }))
                  }
                  placeholder={
                    editForm.role === "TEACHER"
                      ? "Teachers use email verification instead of passwords"
                      : "Enter new password (leave blank to keep current)"
                  }
                  disabled={editForm.role === "TEACHER"}
                  className="flex-1"
                />
              </div>
              {editForm.role === "TEACHER" && (
                <p className="text-xs text-muted-foreground mt-2">
                  Teachers authenticate via email verification links, not
                  passwords
                </p>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
