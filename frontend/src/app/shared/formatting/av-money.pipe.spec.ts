import { AvMoneyPipe } from './av-money.pipe';

describe('AvMoneyPipe', () => {
  const pipe = new AvMoneyPipe();

  it('formats INR professionally using the requested locale', () => {
    expect(pipe.transform(97400, 'INR', 'en-IN')).toBe('₹97,400');
  });

  it('formats USD professionally using the requested locale', () => {
    expect(pipe.transform(97400, 'USD', 'en-US')).toBe('$97,400');
  });

  it('formats EUR professionally using the requested locale', () => {
    expect(pipe.transform(97400, 'EUR', 'en')).toBe('€97,400');
  });

  it('uses a readable amount and code when currency formatting is unsupported', () => {
    expect(pipe.transform(97400, 'NOT_A_CURRENCY', 'en')).toBe('97,400 NOT_A_CURRENCY');
  });

  it('never concatenates the amount and currency fallback without spacing', () => {
    expect(pipe.transform(97400, 'NOT_A_CURRENCY', 'en')).not.toContain('97400NOT_A_CURRENCY');
  });
});