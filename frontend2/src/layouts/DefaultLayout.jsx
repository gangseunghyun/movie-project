import { useLocation } from 'react-router-dom';
import Header from '../components/Header/Header';
import Footer from '../components/Footer/Footer';

export default function DefaultLayout({ children }) {
  const location = useLocation();
  const isMainOrSearchPage = location.pathname === '/' || location.pathname.startsWith('/search');

  return (
    <div className="app-root" style={{
      display: 'flex',
      flexDirection: 'column',
      minHeight: '100vh',
      background: 'var(--color-bg)',
    }}>
      <Header />
      <main style={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        ...(isMainOrSearchPage ? {} : { alignItems: 'center' })
      }}>
        {children}
      </main>
      <Footer />
    </div>
  );
}
