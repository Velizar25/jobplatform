import { useEffect, useState } from 'react'
import './App.css'

const API_URL = 'http://localhost:8080/api'

function App() {
  const [user, setUser] = useState(null)
  const [activePage, setActivePage] = useState('jobs')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const [loginMode, setLoginMode] = useState('login')
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('1234')
  const [registerEmail, setRegisterEmail] = useState('')
  const [registerUsername, setRegisterUsername] = useState('')
  const [registerPassword, setRegisterPassword] = useState('')

  const [jobs, setJobs] = useState([])
  const [selectedJob, setSelectedJob] = useState(null)
  const [jobForm, setJobForm] = useState({
    title: '',
    company: '',
    location: '',
    employmentType: '',
    requiredSkills: '',
    description: '',
  })

  const [cvs, setCvs] = useState([])
  const [cvFile, setCvFile] = useState(null)
  const [cvInputKey, setCvInputKey] = useState(Date.now())
  const [applyCvId, setApplyCvId] = useState('')
  const [applyFile, setApplyFile] = useState(null)
  const [applyInputKey, setApplyInputKey] = useState(Date.now())

  const [applications, setApplications] = useState([])

  const [profile, setProfile] = useState({
    email: '',
    password: '',
    confirmPassword: '',
    fullName: '',
    skills: '',
    preferredLocation: '',
    preferredJobType: '',
  })

  const [chatMessage, setChatMessage] = useState('')
  const [chatResponse, setChatResponse] = useState('')

  const isAdmin = user?.role === 'ROLE_ADMIN' || user?.role === 'ADMIN'

  function clearAlerts() {
    setError('')
    setMessage('')
  }

  async function api(path, options = {}) {
    const response = await fetch(`${API_URL}${path}`, {
      credentials: 'include',
      ...options,
    })

    if (!response.ok) {
      let text = 'Request failed'

      try {
        const data = await response.json()
        text = data.message || data.error || JSON.stringify(data)
      } catch {
        try {
          text = await response.text()
        } catch {
          text = 'Request failed'
        }
      }

      throw new Error(text || 'Request failed')
    }

    const contentType = response.headers.get('content-type') || ''

    if (contentType.includes('application/json')) {
      return response.json()
    }

    return response.text()
  }

  async function checkCurrentUser() {
    try {
      const data = await api('/auth/me')
      setUser(data)
      setActivePage('jobs')
      await loadJobs()
    } catch {
      setUser(null)
    }
  }

  async function handleLogin(e) {
    e.preventDefault()
    clearAlerts()

    try {
      setLoading(true)

      const body = new URLSearchParams()
      body.append('username', username)
      body.append('password', password)

      await api('/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body,
      })

      await checkCurrentUser()
    } catch (err) {
      setError(err.message || 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  async function handleRegister(e) {
    e.preventDefault()
    clearAlerts()

    try {
      setLoading(true)

      await api('/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: registerUsername,
          email: registerEmail,
          password: registerPassword,
        }),
      })

      setMessage('Registration successful. Now login.')
      setLoginMode('login')
      setUsername(registerUsername)
      setPassword('')
      setRegisterEmail('')
      setRegisterUsername('')
      setRegisterPassword('')
    } catch (err) {
      setError(err.message || 'Registration failed')
    } finally {
      setLoading(false)
    }
  }

  async function handleLogout() {
    await fetch(`${API_URL}/auth/logout`, {
      method: 'POST',
      credentials: 'include',
    })

    setUser(null)
    setJobs([])
    setCvs([])
    setApplications([])
    setSelectedJob(null)
    setChatResponse('')
  }

  async function loadJobs() {
    setLoading(true)

    try {
      const data = await api('/jobs')
      setJobs(Array.isArray(data) ? data : [])
    } catch (err) {
      setError(err.message || 'Cannot load jobs')
    } finally {
      setLoading(false)
    }
  }

  async function saveJob(e) {
    e.preventDefault()
    clearAlerts()

    try {
      setLoading(true)

      const payload = {
        title: jobForm.title,
        company: jobForm.company,
        location: jobForm.location,
        employmentType: jobForm.employmentType,
        requiredSkills: jobForm.requiredSkills,
        description: jobForm.description,
      }

      if (jobForm.id) {
        await api(`/jobs/admin/${jobForm.id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        })

        setMessage('Job updated successfully.')
      } else {
        await api('/jobs/admin', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        })

        setMessage('Job created successfully.')
      }

      clearJobForm()
      await loadJobs()
      setActivePage('jobs')
    } catch (err) {
      setError(err.message || 'Job save failed. Admin role required.')
    } finally {
      setLoading(false)
    }
  }

  function editJob(job) {
    setSelectedJob(null)
    setJobForm({
      id: job.id,
      title: job.title || '',
      company: job.company || '',
      location: job.location || '',
      employmentType: job.employmentType || '',
      requiredSkills: job.requiredSkills || '',
      description: job.description || '',
    })

    setActivePage('job-admin')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function clearJobForm() {
    setJobForm({
      title: '',
      company: '',
      location: '',
      employmentType: '',
      requiredSkills: '',
      description: '',
    })
  }

  async function deleteJob(id) {
    if (!window.confirm('Delete this job offer?')) return

    clearAlerts()

    try {
      await api(`/jobs/admin/${id}/delete`, {
        method: 'POST',
      })

      setMessage('Job deleted successfully.')
      await loadJobs()
    } catch (err) {
      setError(err.message || 'Delete failed. Admin role required.')
    }
  }

  async function clearAllJobs() {
    if (!window.confirm('Move all jobs to recycle bin?')) return

    clearAlerts()

    try {
      await api('/jobs/admin/clear', {
        method: 'POST',
      })

      setMessage('All jobs moved to recycle bin.')
      await loadJobs()
    } catch (err) {
      setError(err.message || 'Clear failed. Admin role required.')
    }
  }

  async function restoreAllJobs() {
    clearAlerts()

    try {
      await api('/jobs/admin/restore-all', {
        method: 'POST',
      })

      setMessage('All jobs restored successfully.')
      await loadJobs()
    } catch (err) {
      setError(err.message || 'Restore failed. Admin role required.')
    }
  }

  async function loadCvs() {
    setLoading(true)

    try {
      const data = await api('/cvs')
      setCvs(Array.isArray(data) ? data : [])
    } catch (err) {
      setError(err.message || 'Cannot load CVs')
    } finally {
      setLoading(false)
    }
  }

  async function uploadCv(e) {
    e.preventDefault()
    clearAlerts()

    if (!cvFile) {
      setError('Choose a CV file first.')
      return
    }

    try {
      setLoading(true)

      const formData = new FormData()
      formData.append('cv', cvFile)

      await api('/cvs/upload', {
        method: 'POST',
        body: formData,
      })

      setCvFile(null)
      setCvInputKey(Date.now())
      setMessage('CV uploaded successfully.')
      await loadCvs()
    } catch (err) {
      setError(err.message || 'CV upload failed')
    } finally {
      setLoading(false)
    }
  }

  async function deleteCv(id) {
    if (!window.confirm('Delete this CV?')) return

    clearAlerts()

    try {
      await api(`/cvs/${id}`, {
        method: 'DELETE',
      })

      setMessage('CV deleted successfully.')
      await loadCvs()
    } catch (err) {
      setError(err.message || 'CV delete failed')
    }
  }

  async function applyForJob(e) {
    e.preventDefault()
    clearAlerts()

    if (!selectedJob) return

    if (!applyCvId && !applyFile) {
      setError('Choose existing CV or upload a new CV.')
      return
    }

    try {
      setLoading(true)

      const formData = new FormData()

      if (applyCvId) {
        formData.append('cvId', applyCvId)
      }

      if (applyFile) {
        formData.append('cv', applyFile)
      }

      await api(`/jobs/${selectedJob.id}/apply`, {
        method: 'POST',
        body: formData,
      })

      setMessage('Application submitted successfully.')
      setSelectedJob(null)
      setApplyCvId('')
      setApplyFile(null)
      setApplyInputKey(Date.now())
      await loadApplications()
    } catch (err) {
      setError(err.message || 'Application failed')
    } finally {
      setLoading(false)
    }
  }

  async function loadApplications() {
    setLoading(true)

    try {
      const data = await api('/applications')
      setApplications(Array.isArray(data) ? data : [])
    } catch (err) {
      setError(err.message || 'Cannot load applications')
    } finally {
      setLoading(false)
    }
  }

  async function deleteApplication(id) {
    if (!window.confirm('Delete this application?')) return

    clearAlerts()

    try {
      await api(`/applications/${id}`, {
        method: 'DELETE',
      })

      setMessage('Application deleted successfully.')
      await loadApplications()
    } catch (err) {
      setError(err.message || 'Application delete failed')
    }
  }

  async function loadProfile() {
    setLoading(true)

    try {
      const data = await api('/profile')

      setProfile({
        email: data.email || '',
        password: '',
        confirmPassword: '',
        fullName: data.fullName || '',
        skills: data.skills || '',
        preferredLocation: data.preferredLocation || '',
        preferredJobType: data.preferredJobType || '',
      })
    } catch (err) {
      setError(err.message || 'Cannot load profile')
    } finally {
      setLoading(false)
    }
  }

  async function saveProfile(e) {
    e.preventDefault()
    clearAlerts()

    try {
      setLoading(true)

      await api('/profile', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(profile),
      })

      setMessage('Profile updated successfully.')
      await checkCurrentUser()
      await loadProfile()
    } catch (err) {
      setError(err.message || 'Profile update failed')
    } finally {
      setLoading(false)
    }
  }

  async function askChatbot(e) {
    e.preventDefault()

    if (!chatMessage.trim()) return

    setChatResponse('')
    clearAlerts()

    try {
      setLoading(true)

      const data = await api('/chatbot/ask', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: chatMessage }),
      })

      setChatResponse(data.response || data.message || String(data))
    } catch (err) {
      setChatResponse(err.message || 'Chatbot failed')
    } finally {
      setLoading(false)
    }
  }

  async function openJobModal(job) {
    clearAlerts()
    setSelectedJob(job)
    setApplyCvId('')
    setApplyFile(null)
    setApplyInputKey(Date.now())
    await loadCvs()
  }

  async function openPage(page) {
    setActivePage(page)
    clearAlerts()
    setSelectedJob(null)

    if (page === 'jobs') await loadJobs()
    if (page === 'cvs') await loadCvs()
    if (page === 'applications') await loadApplications()
    if (page === 'profile') await loadProfile()
  }

  function truncate(text, maxLength = 185) {
    if (!text) return 'No description available.'
    if (text.length <= maxLength) return text
    return `${text.substring(0, maxLength)}...`
  }

  useEffect(() => {
    checkCurrentUser()
  }, [])

  if (!user) {
    return (
      <div className="login-page">
        <div className="login-card">
          <h1>JobPlatform</h1>

          {loginMode === 'login' ? (
            <>
              <p>Login to access the job platform.</p>

              <form onSubmit={handleLogin} className="login-form">
                <label>Username or Email</label>
                <input value={username} onChange={(e) => setUsername(e.target.value)} />

                <label>Password</label>
                <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />

                {error && <div className="login-error">{error}</div>}
                {message && <div className="success-box">{message}</div>}

                <button type="submit" disabled={loading}>
                  {loading ? 'Loading...' : 'Login'}
                </button>
              </form>

              <button className="switch-btn" onClick={() => setLoginMode('register')}>
                Create new account
              </button>

              <a className="google-login" href="http://localhost:8080/oauth2/authorization/google">
                Login with Google
              </a>
            </>
          ) : (
            <>
              <p>Create a new candidate account.</p>

              <form onSubmit={handleRegister} className="login-form">
                <label>Username</label>
                <input value={registerUsername} onChange={(e) => setRegisterUsername(e.target.value)} />

                <label>Email</label>
                <input value={registerEmail} onChange={(e) => setRegisterEmail(e.target.value)} />

                <label>Password</label>
                <input
                  type="password"
                  value={registerPassword}
                  onChange={(e) => setRegisterPassword(e.target.value)}
                />

                {error && <div className="login-error">{error}</div>}

                <button type="submit" disabled={loading}>
                  {loading ? 'Loading...' : 'Register'}
                </button>
              </form>

              <button className="switch-btn" onClick={() => setLoginMode('login')}>
                Back to login
              </button>
            </>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="app">
      <nav className="navbar">
        <button className="logo" onClick={() => openPage('jobs')}>
          JobPlatform
        </button>

        <div className="nav-links">
          <button className={activePage === 'jobs' ? 'active' : ''} onClick={() => openPage('jobs')}>
            Jobs
          </button>
          <button
            className={activePage === 'applications' ? 'active' : ''}
            onClick={() => openPage('applications')}
          >
            Applications
          </button>
          <button className={activePage === 'cvs' ? 'active' : ''} onClick={() => openPage('cvs')}>
            CVs
          </button>
          <button className={activePage === 'chatbot' ? 'active' : ''} onClick={() => openPage('chatbot')}>
            Chatbot
          </button>
          <button className={activePage === 'profile' ? 'active' : ''} onClick={() => openPage('profile')}>
            Profile
          </button>

          {isAdmin && (
            <button
              className={activePage === 'job-admin' ? 'active' : ''}
              onClick={() => {
                clearJobForm()
                clearAlerts()
                setSelectedJob(null)
                setActivePage('job-admin')
              }}
            >
              New Job
            </button>
          )}

          <span className="user-badge">{user.username || user.email}</span>
          <button className="logout-btn" onClick={handleLogout}>Logout</button>
        </div>
      </nav>

      <header className="hero-section">
        <div className="hero-inner">
          <h1>Find your next job with intelligent assistance</h1>
          <p>
            A modern job application platform with CV upload, applications,
            admin job management and an intelligent chatbot assistant.
          </p>

          <div className="hero-buttons">
            <button onClick={() => openPage('jobs')}>Browse Jobs</button>
            <button className="secondary" onClick={() => openPage('chatbot')}>
              Ask Chatbot
            </button>
          </div>
        </div>
      </header>

      <main className="content">
        {message && <div className="success-box global-alert">{message}</div>}
        {error && <div className="error global-alert">{error}</div>}

        {activePage === 'jobs' && (
          <>
            <div className="section-title">
              <h2>Available Job Offers</h2>
              <p>Choose a position and apply with your CV.</p>
            </div>

            {isAdmin && (
              <div className="admin-actions">
                <button
                  onClick={() => {
                    clearJobForm()
                    clearAlerts()
                    setSelectedJob(null)
                    setActivePage('job-admin')
                  }}
                >
                  Add New Job
                </button>
                <button onClick={clearAllJobs}>Clear All Jobs</button>
                <button onClick={restoreAllJobs}>Restore All Jobs</button>
              </div>
            )}

            {loading && <p className="info">Loading...</p>}

            <div className="jobs-grid">
              {jobs.map((job) => (
                <div className="job-card" key={job.id}>
                  <div className="job-header">
                    <h3>{job.title}</h3>
                    <span>{job.employmentType || 'Not specified'}</span>
                  </div>

                  <p className="company">{job.company || 'Company not specified'}</p>
                  <p className="location">{job.location || 'Location not specified'}</p>

                  {job.requiredSkills && (
                    <p className="skills">
                      <strong>Required skills:</strong> {job.requiredSkills}
                    </p>
                  )}

                  <p className="description">{truncate(job.description)}</p>

                  <div className="job-card-footer">
                    <button className="apply-btn" onClick={() => openJobModal(job)}>
                      View Details / Apply
                    </button>

                    {isAdmin && (
                      <div className="card-actions">
                        <button onClick={() => editJob(job)}>Edit</button>
                        <button className="danger-btn" onClick={() => deleteJob(job.id)}>Delete</button>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>

            {!loading && jobs.length === 0 && (
              <div className="empty-state">No job offers available.</div>
            )}
          </>
        )}

        {activePage === 'job-admin' && (
          <div className="page-card">
            <div className="section-title left">
              <h2>{jobForm.id ? 'Edit Job Offer' : 'Create New Job Offer'}</h2>
              <p>{jobForm.id ? 'Update the selected job offer.' : 'Create a new job offer for candidates.'}</p>
            </div>

            <form className="form-grid" onSubmit={saveJob}>
              <div className="form-group">
                <label>Title</label>
                <input
                  value={jobForm.title}
                  onChange={(e) => setJobForm({ ...jobForm, title: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label>Company</label>
                <input
                  value={jobForm.company}
                  onChange={(e) => setJobForm({ ...jobForm, company: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label>Location</label>
                <input
                  value={jobForm.location}
                  onChange={(e) => setJobForm({ ...jobForm, location: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label>Employment Type</label>
                <input
                  value={jobForm.employmentType}
                  onChange={(e) => setJobForm({ ...jobForm, employmentType: e.target.value })}
                  placeholder="Full-time, Internship, Remote..."
                />
              </div>

              <div className="form-group full-width">
                <label>Required Skills</label>
                <input
                  value={jobForm.requiredSkills}
                  onChange={(e) => setJobForm({ ...jobForm, requiredSkills: e.target.value })}
                  placeholder="Java, Spring Boot, React..."
                />
              </div>

              <div className="form-group full-width">
                <label>Description</label>
                <textarea
                  value={jobForm.description}
                  onChange={(e) => setJobForm({ ...jobForm, description: e.target.value })}
                  required
                />
              </div>

              <div className="form-actions full-width">
                <button type="submit" className="primary-btn">
                  {jobForm.id ? 'Save Changes' : 'Create Job'}
                </button>

                <button
                  type="button"
                  className="secondary-light-btn"
                  onClick={() => {
                    clearJobForm()
                    setActivePage('jobs')
                  }}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}

        {activePage === 'cvs' && (
          <div className="page-card">
            <div className="section-title left">
              <h2>CV Management</h2>
              <p>Upload, download and delete your CV files.</p>
            </div>

            <form className="upload-card" onSubmit={uploadCv}>
              <div>
                <label>Upload CV PDF/DOC/DOCX</label>
                <input
                  key={cvInputKey}
                  type="file"
                  accept=".pdf,.doc,.docx"
                  onChange={(e) => setCvFile(e.target.files[0] || null)}
                />
              </div>

              <button type="submit" className="primary-btn">
                Upload CV
              </button>
            </form>

            <h3 className="subheading">My CVs</h3>

            <div className="list-box">
              {cvs.map((cv) => (
                <div className="list-row" key={cv.id}>
                  <div>
                    <strong>{cv.filename}</strong>
                    <p>{cv.fileType || 'CV document'}</p>
                  </div>

                  <div className="row-actions">
                    <a
                      className="small-btn"
                      href={`${API_URL}/cvs/${cv.id}/download`}
                      target="_blank"
                      rel="noreferrer"
                    >
                      Download
                    </a>

                    <button className="danger-btn" onClick={() => deleteCv(cv.id)}>
                      Delete
                    </button>
                  </div>
                </div>
              ))}

              {cvs.length === 0 && <p className="empty-state">No CVs uploaded yet.</p>}
            </div>
          </div>
        )}

        {activePage === 'applications' && (
          <div className="page-card">
            <div className="section-title left">
              <h2>Applications</h2>
              <p>
                {isAdmin
                  ? 'Admin view: review and delete submitted applications.'
                  : 'Review your submitted applications.'}
              </p>
            </div>

            {loading && <p className="info">Loading...</p>}

            <div className="applications-list">
              {applications.map((app) => (
                <div className="application-card" key={app.id}>
                  <div>
                    <h3>{app.jobTitle || app.job?.title || 'Job application'}</h3>
                    <p><strong>Email:</strong> {app.email || 'Not specified'}</p>
                    <p><strong>Status:</strong> {app.status || 'Submitted'}</p>

                    {isAdmin && (
                      <p>
                        <strong>Candidate:</strong>{' '}
                        {app.applicantUsername || app.username || app.applicant?.username || 'Not specified'}
                      </p>
                    )}
                  </div>

                  {isAdmin && (
                    <button className="danger-btn" onClick={() => deleteApplication(app.id)}>
                      Delete Application
                    </button>
                  )}
                </div>
              ))}

              {!loading && applications.length === 0 && (
                <p className="empty-state">No applications found.</p>
              )}
            </div>
          </div>
        )}

        {activePage === 'profile' && (
          <div className="page-card">
            <div className="section-title left">
              <h2>Edit Profile</h2>
              <p>Update your personal and professional information.</p>
            </div>

            <form className="form-grid" onSubmit={saveProfile}>
              <div className="form-group">
                <label>Email</label>
                <input
                  value={profile.email}
                  onChange={(e) => setProfile({ ...profile, email: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Full Name</label>
                <input
                  value={profile.fullName}
                  onChange={(e) => setProfile({ ...profile, fullName: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Skills</label>
                <input
                  value={profile.skills}
                  onChange={(e) => setProfile({ ...profile, skills: e.target.value })}
                  placeholder="Java, React, SQL..."
                />
              </div>

              <div className="form-group">
                <label>Preferred Location</label>
                <input
                  value={profile.preferredLocation}
                  onChange={(e) => setProfile({ ...profile, preferredLocation: e.target.value })}
                  placeholder="Sofia, Remote..."
                />
              </div>

              <div className="form-group">
                <label>Preferred Job Type</label>
                <input
                  value={profile.preferredJobType}
                  onChange={(e) => setProfile({ ...profile, preferredJobType: e.target.value })}
                  placeholder="Full-time, Remote..."
                />
              </div>

              <div className="form-group">
                <label>New Password</label>
                <input
                  type="password"
                  value={profile.password}
                  onChange={(e) => setProfile({ ...profile, password: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Confirm Password</label>
                <input
                  type="password"
                  value={profile.confirmPassword}
                  onChange={(e) => setProfile({ ...profile, confirmPassword: e.target.value })}
                />
              </div>

              <div className="form-actions full-width">
                <button type="submit" className="primary-btn">
                  Save Profile
                </button>
              </div>
            </form>
          </div>
        )}

        {activePage === 'chatbot' && (
          <div className="page-card chatbot-page">
            <div className="section-title left">
              <h2>Intelligent Chatbot Assistant</h2>
              <p>Ask questions about jobs, CVs, applications, interviews or profile settings.</p>
            </div>

            <form className="chat-form" onSubmit={askChatbot}>
              <input
                value={chatMessage}
                onChange={(e) => setChatMessage(e.target.value)}
                placeholder="Example: recommend jobs for Java"
              />
              <button type="submit" className="primary-btn">
                Ask
              </button>
            </form>

            {chatResponse && (
              <div className="chat-response">
                <strong>Assistant:</strong>
                <p>{chatResponse}</p>
              </div>
            )}
          </div>
        )}
      </main>

      {selectedJob && (
        <div className="modal-backdrop" onClick={() => setSelectedJob(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <button className="modal-close" onClick={() => setSelectedJob(null)}>
              ×
            </button>

            <div className="modal-job-header">
              <div>
                <h2>{selectedJob.title}</h2>
                <p className="company modal-company">{selectedJob.company}</p>
              </div>

              <span>{selectedJob.employmentType || 'Not specified'}</span>
            </div>

            <div className="job-meta-grid">
              <p><strong>Location:</strong> {selectedJob.location || 'Not specified'}</p>
              <p><strong>Skills:</strong> {selectedJob.requiredSkills || 'Not specified'}</p>
            </div>

            <p className="job-description-full">
              {selectedJob.description || 'No description available.'}
            </p>

            <form className="apply-box" onSubmit={applyForJob}>
              <h3>Apply for this job</h3>

              <div className="form-group">
                <label>Choose existing CV</label>
                <select value={applyCvId} onChange={(e) => setApplyCvId(e.target.value)}>
                  <option value="">No existing CV selected</option>
                  {cvs.map((cv) => (
                    <option key={cv.id} value={cv.id}>
                      {cv.filename}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Or upload new CV</label>
                <input
                  key={applyInputKey}
                  type="file"
                  accept=".pdf,.doc,.docx"
                  onChange={(e) => setApplyFile(e.target.files[0] || null)}
                />
              </div>

              <div className="form-actions">
                <button type="submit" className="primary-btn">
                  Apply
                </button>
                <button type="button" className="secondary-light-btn" onClick={() => setSelectedJob(null)}>
                  Close
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

export default App