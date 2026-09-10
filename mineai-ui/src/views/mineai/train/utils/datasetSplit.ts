export const DEFAULT_LEGACY_SPLIT_SIZE = '8-1-1';

export const splitRangeLimits = {
  min: 1,
  max: 99,
};

export function normalizeSplitRange(value: number[]): [number, number] {
  const fallback: [number, number] = [80, 90];
  const rawFirst = Number(value?.[0] ?? fallback[0]);
  const rawSecond = Number(value?.[1] ?? fallback[1]);
  const first = Math.max(
    splitRangeLimits.min,
    Math.min(98, Math.round(Number.isFinite(rawFirst) ? rawFirst : fallback[0])),
  );
  const second = Math.max(
    first + splitRangeLimits.min,
    Math.min(splitRangeLimits.max, Math.round(Number.isFinite(rawSecond) ? rawSecond : fallback[1])),
  );
  return [first, second];
}

export function splitRangeToPercentString(value: number[]): string {
  const [train, second] = normalizeSplitRange(value);
  const test = second - train;
  const val = 100 - second;
  return `${train}-${test}-${val}`;
}

export function splitStringToPercentParts(value?: string | null): [number, number, number] {
  const rawParts = String(value || DEFAULT_LEGACY_SPLIT_SIZE)
    .split('-')
    .map((item) => Number(item));

  if (rawParts.length !== 3 || rawParts.some((item) => !Number.isFinite(item) || item <= 0)) {
    return [80, 10, 10];
  }

  const total = rawParts.reduce((sum, item) => sum + item, 0);
  if (Math.abs(total - 100) < 0.000001) {
    return rawParts.map((item) => Math.round(item)) as [number, number, number];
  }

  if (Math.abs(total - 10) < 0.000001) {
    return rawParts.map((item) => Math.round(item * 10)) as [number, number, number];
  }

  return rawParts.map((item) => Math.max(1, Math.round((item / total) * 100))) as [
    number,
    number,
    number,
  ];
}

export function splitStringToRange(value?: string | null): [number, number] {
  const [train, test] = splitStringToPercentParts(value);
  return normalizeSplitRange([train, train + test]);
}

export function normalizeSplitStringForBackend(value?: string | null): string {
  const [train, test, val] = splitStringToPercentParts(value);
  return `${train}-${test}-${val}`;
}
