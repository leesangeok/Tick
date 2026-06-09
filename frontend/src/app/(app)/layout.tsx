import { AppSidebar } from "@/components/layout/AppSidebar";
import { TopNavigation } from "@/components/layout/TopNavigation";
import { MobileNavigation } from "@/components/layout/MobileNavigation";

export default function AppLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen">
      <AppSidebar />
      <div className="flex flex-1 flex-col">
        <TopNavigation />
        <main className="flex-1 px-4 pb-24 pt-6 md:px-8 md:pb-8">
          {children}
        </main>
        <MobileNavigation />
      </div>
    </div>
  );
}
