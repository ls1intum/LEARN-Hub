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
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { AlertTriangle, CheckCircle, Loader2, Trash2 } from "lucide-react";
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
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [userToDelete, setUserToDelete] = useState<UserType | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

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

  const openDeleteDialog = (user: UserType) => {
    setUserToDelete(user);
    setDeleteDialogOpen(true);
    setError(null);
    setMessage("");
  };

  const closeDeleteDialog = () => {
    setDeleteDialogOpen(false);
    setUserToDelete(null);
    setError(null);
  };

  const getErrorMessage = (err: unknown): string => {
    if (err instanceof Error) {
      const message = err.message.toLowerCase();

      // Handle specific error cases
      if (message.includes("cannot delete your own account")) {
        return "You cannot delete your own account. Please ask another admin to delete it.";
      }
      if (message.includes("not found")) {
        return "User not found. It may have already been deleted.";
      }
      if (message.includes("foreign key") || message.includes("constraint")) {
        return "Unable to delete user due to database constraints. Please contact support if this persists.";
      }
      if (message.includes("network") || message.includes("fetch")) {
        return "Network error. Please check your connection and try again.";
      }
      if (message.includes("401") || message.includes("unauthorized")) {
        return "You don't have permission to delete users. Please contact an administrator.";
      }
      if (message.includes("403") || message.includes("forbidden")) {
        return "You don't have permission to delete users.";
      }
      if (message.includes("500") || message.includes("server")) {
        return "Server error occurred. Please try again later or contact support.";
      }

      // Return the original message if it's meaningful
      return err.message;
    }
    return "An unexpected error occurred. Please try again.";
  };

  const handleDeleteUser = async () => {
    if (!userToDelete) return;

    setIsDeleting(true);
    setError(null);
    setMessage("");

    try {
      await apiService.deleteUser(userToDelete.id);
      setMessage(`User "${userToDelete.email}" deleted successfully`);
      closeDeleteDialog();
      fetchUsers(); // Refresh the list
    } catch (err) {
      logger.error("Error deleting user", err, "UserManagementPage");
      const errorMessage = getErrorMessage(err);
      setError(errorMessage);
    } finally {
      setIsDeleting(false);
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
          <Alert className="mb-4 border-green-500/50 bg-green-500/10">
            <CheckCircle className="h-4 w-4 text-green-600" />
            <AlertDescription className="text-green-700">
              {message}
            </AlertDescription>
          </Alert>
        )}
        {error && (
          <Alert variant="destructive" className="mb-4">
            <AlertTriangle className="h-4 w-4" />
            <AlertDescription>{error}</AlertDescription>
          </Alert>
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
                              onClick={() => openDeleteDialog(user)}
                              size="sm"
                              className="bg-destructive hover:bg-destructive/80"
                              disabled={isDeleting}
                            >
                              <Trash2 className="h-3 w-3 mr-1" />
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

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <AlertTriangle className="h-5 w-5 text-destructive" />
              Delete User
            </DialogTitle>
            <DialogDescription>
              Are you sure you want to delete this user? This action cannot be
              undone.
            </DialogDescription>
          </DialogHeader>

          {userToDelete && (
            <div className="py-4">
              <div className="p-3 bg-muted rounded-lg space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium">Email:</span>
                  <span className="text-sm">{userToDelete.email}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium">Name:</span>
                  <span className="text-sm">
                    {userToDelete.first_name} {userToDelete.last_name}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium">Role:</span>
                  <span className="text-sm">{userToDelete.role}</span>
                </div>
              </div>

              <div className="mt-4 p-3 bg-destructive/10 border border-destructive/30 rounded-lg">
                <p className="text-sm font-medium text-destructive mb-2">
                  This will permanently delete:
                </p>
                <ul className="text-xs text-muted-foreground space-y-1 ml-4 list-disc">
                  <li>User account and profile information</li>
                  <li>All saved favorites and lesson plans</li>
                  <li>Search history</li>
                  <li>Verification codes</li>
                  <li>All other personal data</li>
                </ul>
              </div>
            </div>
          )}

          {error && (
            <Alert variant="destructive" className="mt-2">
              <AlertTriangle className="h-4 w-4" />
              <AlertDescription className="text-xs">{error}</AlertDescription>
            </Alert>
          )}

          <DialogFooter>
            <Button
              variant="outline"
              onClick={closeDeleteDialog}
              disabled={isDeleting}
            >
              Cancel
            </Button>
            <Button
              variant="destructive"
              onClick={handleDeleteUser}
              disabled={isDeleting}
            >
              {isDeleting ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Deleting...
                </>
              ) : (
                <>
                  <Trash2 className="h-4 w-4 mr-2" />
                  Delete User
                </>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};
