import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import DefaultLayout from './layouts/DefaultLayout';
import LoginPage from './components/LoginPage/LoginPage';
import SignupPage from './components/SignupPage/SignupPage';
import FilmerLoginPage from './components/FilmerLoginPage/FilmerLoginPage';
import FilmerExtraInfoPage from './components/LoginPage/SocialJoinComplete';
import { ToastContainer } from 'react-toastify';
import { UserProvider } from './contexts/UserContext';
import 'react-toastify/dist/ReactToastify.css';
import FindIdPage from './components/LoginPage/FindIdPage';
import FindIdResultPage from './components/LoginPage/FindIdResultPage';
import FindPasswordPage from './components/LoginPage/FindPasswordPage';
import ResetPasswordPage from './components/LoginPage/ResetPasswordPage';
import MainPage from './components/MainPage/MainPage';
import SearchResultPage from './components/SearchResultPage/SearchResultPage';
import MovieDetailPage from './components/MovieDetailPage/MovieDetailPage';
import DirectorDetailPage from './components/PersonDetailPage/DirectorDetailPage';
import ActorDetailPage from './components/PersonDetailPage/ActorDetailPage';
import MyPage from './components/MyPage/MyPage';
import ProfileEditPage from './components/MyPage/ProfileEditPage';
import BookingPage from './components/BookingPage/BookingPage';
import ReservationPage from './components/ReservationPage/ReservationPage';
import ReservationDetailPage from './components/ReservationPage/ReservationDetailPage';
import MovieEditPage from './components/Admin/MovieEditPage';
import MovieRegisterPage from './components/Admin/MovieRegisterPage';

export default function App() {
  return (
    <UserProvider>
      <BrowserRouter>
        <ToastContainer position="top-center" autoClose={2000} />
        <DefaultLayout>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/loginform" element={<FilmerLoginPage />} />
            <Route path="/signup" element={<SignupPage />} />
            <Route path="/social-join" element={<FilmerExtraInfoPage />} />
            <Route path="/find-id" element={<FindIdPage />} />
            <Route path="/find-id-result" element={<FindIdResultPage />} />
            <Route path="/find-pw" element={<FindPasswordPage />} />
            <Route path="/reset-password" element={<ResetPasswordPage />} />
            <Route path="/" element={<MainPage />} />
            <Route path="/search" element={<SearchResultPage />} />
            <Route path="/movie-detail/:movieCd" element={<MovieDetailPage />} />
            <Route path="/booking/:movieId" element={<BookingPage />} />
            <Route path="/reservations" element={<ReservationPage />} />
                    <Route path="/reservations/:reservationId" element={<ReservationDetailPage />} />
            <Route path="/admin/movie/edit/:movieId" element={<MovieEditPage />} />
            <Route path="/admin/movie/register" element={<MovieRegisterPage />} />
            <Route path="/person/director/:id" element={<DirectorDetailPage />} />
            <Route path="/person/actor/:id" element={<ActorDetailPage />} />
            <Route path="/mypage/:userId" element={<MyPage />} />
            <Route path="/profile-edit" element={<ProfileEditPage />} />
          </Routes>
        </DefaultLayout>
      </BrowserRouter>
    </UserProvider>
  );
}
