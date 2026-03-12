import React, { useState } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ActivityFavouritesTab } from "@/components/favourites/ActivityFavouritesTab";
import { LessonPlanFavouritesTab } from "@/components/favourites/LessonPlanFavouritesTab";

export const FavouritesPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState("activities");

  return (
    <div className="py-6">
      <div className="mb-6">
        <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
          Favourites
        </h1>
        <p className="text-muted-foreground mt-1.5 text-sm sm:text-base">
          Manage your favourite activities and lesson plans
        </p>
      </div>

      <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="activities">Activities</TabsTrigger>
          <TabsTrigger value="lesson-plans">Lesson Plans</TabsTrigger>
        </TabsList>

        <TabsContent value="activities" className="mt-6">
          <ActivityFavouritesTab />
        </TabsContent>

        <TabsContent value="lesson-plans" className="mt-6">
          <LessonPlanFavouritesTab />
        </TabsContent>
      </Tabs>
    </div>
  );
};
