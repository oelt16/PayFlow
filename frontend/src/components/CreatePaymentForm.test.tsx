import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { CreatePaymentForm } from './CreatePaymentForm'

afterEach(() => {
  cleanup()
})

describe('CreatePaymentForm', () => {
  const mockOnSubmit = vi.fn().mockResolvedValue(undefined)

  it('renders all form fields', () => {
    render(<CreatePaymentForm onSubmit={mockOnSubmit} />)

    expect(screen.getByLabelText(/amount/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/currency/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/card number/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/expiry month/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/expiry year/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/cvc/i)).toBeInTheDocument()
    // Get all buttons and filter to find the submit button with exact text
    const buttons = screen.getAllByRole('button')
    const submitButton = buttons.find(
      (btn) => btn.textContent === 'Create Payment' && btn.getAttribute('type') === 'submit'
    )
    expect(submitButton).toBeInTheDocument()
  })

  it('shows error when amount is not positive', async () => {
    render(<CreatePaymentForm onSubmit={mockOnSubmit} />)

    const amountInput = screen.getByLabelText(/amount/i)
    fireEvent.change(amountInput, { target: { value: '0' } })

    const buttons = screen.getAllByRole('button')
    const submitButton = buttons.find(
      (btn) => btn.textContent === 'Create Payment' && btn.getAttribute('type') === 'submit'
    )
    fireEvent.click(submitButton!)

    expect(await screen.findByText(/amount must be at least 1/i)).toBeInTheDocument()
    expect(mockOnSubmit).not.toHaveBeenCalled()
  })

  it('shows error when amount is negative', async () => {
    render(<CreatePaymentForm onSubmit={mockOnSubmit} />)

    const amountInput = screen.getByLabelText(/amount/i)
    fireEvent.change(amountInput, { target: { value: '-100' } })

    const buttons = screen.getAllByRole('button')
    const submitButton = buttons.find(
      (btn) => btn.textContent === 'Create Payment' && btn.getAttribute('type') === 'submit'
    )
    fireEvent.click(submitButton!)

    expect(await screen.findByText(/amount must be at least 1/i)).toBeInTheDocument()
    expect(mockOnSubmit).not.toHaveBeenCalled()
  })

  it('shows error when card number is not 16 digits', async () => {
    render(<CreatePaymentForm onSubmit={mockOnSubmit} />)

    const amountInput = screen.getByLabelText(/amount/i)
    fireEvent.change(amountInput, { target: { value: '1000' } })

    const cardInput = screen.getByLabelText(/card number/i)
    fireEvent.change(cardInput, { target: { value: '1234' } })

    const buttons = screen.getAllByRole('button')
    const submitButton = buttons.find(
      (btn) => btn.textContent === 'Create Payment' && btn.getAttribute('type') === 'submit'
    )
    fireEvent.click(submitButton!)

    expect(await screen.findByText(/card number must be exactly 16 digits/i)).toBeInTheDocument()
    expect(mockOnSubmit).not.toHaveBeenCalled()
  })

  it('shows error when cvc is not 3-4 digits', async () => {
    render(<CreatePaymentForm onSubmit={mockOnSubmit} />)

    const amountInput = screen.getByLabelText(/amount/i)
    fireEvent.change(amountInput, { target: { value: '1000' } })

    const cardInput = screen.getByLabelText(/card number/i)
    fireEvent.change(cardInput, { target: { value: '4111111111111111' } })

    const cvcInput = screen.getByLabelText(/cvc/i)
    fireEvent.change(cvcInput, { target: { value: '12' } })

    const buttons = screen.getAllByRole('button')
    const submitButton = buttons.find(
      (btn) => btn.textContent === 'Create Payment' && btn.getAttribute('type') === 'submit'
    )
    fireEvent.click(submitButton!)

    expect(await screen.findByText(/cvc must be 3 or 4 digits/i)).toBeInTheDocument()
    expect(mockOnSubmit).not.toHaveBeenCalled()
  })

  it('calls onSubmit with correct values when form is valid', async () => {
    render(<CreatePaymentForm onSubmit={mockOnSubmit} />)

    // Fill in the form - use userEvent for better simulation
    const amountInput = screen.getByLabelText(/amount/i)
    fireEvent.change(amountInput, { target: { value: '1000' } })

    const cardInput = screen.getByLabelText(/card number/i)
    fireEvent.change(cardInput, { target: { value: '4111111111111111' } })

    const cvcInput = screen.getByLabelText(/cvc/i)
    fireEvent.change(cvcInput, { target: { value: '123' } })

    // Click submit button (same pattern as other tests)
    const buttons = screen.getAllByRole('button')
    const submitButton = buttons.find(
      (btn) => btn.textContent === 'Create Payment' && btn.getAttribute('type') === 'submit'
    )
    fireEvent.click(submitButton!)

    // Verify onSubmit was called with correct values
    // Note: handleSubmit passes (values, event) — we only check the first arg
    await vi.waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalled()
      const [values] = mockOnSubmit.mock.calls[0]
      expect(values).toMatchObject({
        amount: 1000,
        currency: 'USD',
        cardNumber: '4111111111111111',
        cvc: '123',
        expMonth: 1,
        expYear: 2026,
      })
    })
  })

  it('shows loading state when isSubmitting is true', () => {
    render(<CreatePaymentForm onSubmit={mockOnSubmit} isSubmitting={true} />)

    expect(screen.getByRole('button', { name: /creating/i })).toBeDisabled()
  })
})