import { lazy, Suspense } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "./components/AppShell";
import { LoadingState } from "./components/AsyncStates";

const DashboardPage = lazy(() =>
  import("./pages/DashboardPage").then((module) => ({ default: module.DashboardPage })),
);
const WebsitesPage = lazy(() =>
  import("./pages/WebsitesPage").then((module) => ({ default: module.WebsitesPage })),
);
const WebsiteDetailsPage = lazy(() =>
  import("./pages/WebsiteDetailsPage").then((module) => ({ default: module.WebsiteDetailsPage })),
);
const IncidentsPage = lazy(() =>
  import("./pages/IncidentsPage").then((module) => ({ default: module.IncidentsPage })),
);

export default function App() {
  return (
    <Suspense fallback={<div className="p-6"><LoadingState rows={7} /></div>}>
      <Routes>
        <Route element={<AppShell />}>
          <Route index element={<DashboardPage />} />
          <Route path="websites" element={<WebsitesPage />} />
          <Route path="websites/:websiteId" element={<WebsiteDetailsPage />} />
          <Route path="incidents" element={<IncidentsPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
