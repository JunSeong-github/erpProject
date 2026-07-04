import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { router } from './app/router'
import { AuthProvider } from './app/AuthContext'
import { NotificationProvider } from './notifications/NotificationContext'
import './index.css'
import './style.css';

const queryClient = new QueryClient()

createRoot(document.getElementById('root')).render(
    <QueryClientProvider client={queryClient}>
        <AuthProvider>
            <NotificationProvider>
                <RouterProvider router={router} />
            </NotificationProvider>
        </AuthProvider>
    </QueryClientProvider>
)
